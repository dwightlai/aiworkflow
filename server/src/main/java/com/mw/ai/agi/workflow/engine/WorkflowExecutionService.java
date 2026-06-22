package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.chat.service.HumanConfirmService;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowEdge;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowExecutionService {
    private final WorkflowApplicationService workflowService;
    private final WorkflowExecutionStore executionStore;
    private final WorkflowNodeExecutorRegistry executorRegistry;
    private final TenantBusinessGuard tenantGuard;
    private final HumanConfirmService humanConfirmService;

    public WorkflowExecutionService(
            WorkflowApplicationService workflowService,
            WorkflowExecutionStore executionStore,
            WorkflowNodeExecutorRegistry executorRegistry,
            @Autowired(required = false) TenantBusinessGuard tenantGuard,
            @Autowired(required = false) HumanConfirmService humanConfirmService
    ) {
        this.workflowService = workflowService;
        this.executionStore = executionStore;
        this.executorRegistry = executorRegistry;
        this.tenantGuard = tenantGuard;
        this.humanConfirmService = humanConfirmService;
    }

    public WorkflowExecutionResult runWorkflow(WorkflowExecutionRequest request) {
        WorkflowVersion version = workflowService.getPublishedVersion(request.workflowId());
        WorkflowDefinition definition = version.definition();
        WorkflowNode currentNode = findStartNode(definition);
        return runFromNode(request, version, definition, currentNode, new LinkedHashMap<>(request.input()), null);
    }

    public WorkflowExecutionResult resumeAfterConfirm(String executionId, String taskId) {
        WorkflowExecution execution = executionStore.findWorkflowExecutionById(executionId)
                .orElseThrow(() -> new WorkflowRunNotFoundException(executionId));
        if (execution.status() != WorkflowExecutionStatus.WAITING_CONFIRM) {
            throw new IllegalStateException("Workflow execution is not waiting for confirm");
        }
        if (humanConfirmService != null) {
            humanConfirmService.getTask(taskId);
        }
        WorkflowVersion version = workflowService.getPublishedVersion(execution.workflowId());
        WorkflowDefinition definition = version.definition();
        Map<String, WorkflowNode> nodesById = definition.nodes().stream()
                .collect(Collectors.toMap(WorkflowNode::id, Function.identity()));
        WorkflowNode currentNode = nodesById.get(execution.currentNodeId());
        if (currentNode == null) {
            throw new IllegalStateException("Workflow node not found: " + execution.currentNodeId());
        }
        Map<String, Object> context = new LinkedHashMap<>(execution.context());
        context.put("__confirmedTaskId", taskId);
        WorkflowExecutionRequest request = new WorkflowExecutionRequest(execution.workflowId(), execution.input(), Map.of());
        return runFromNode(request, version, definition, currentNode, context, execution);
    }

    public WorkflowExecutionResult rejectAfterConfirm(String executionId, String taskId) {
        WorkflowExecution execution = executionStore.findWorkflowExecutionById(executionId)
                .orElseThrow(() -> new WorkflowRunNotFoundException(executionId));
        WorkflowExecution rejected = new WorkflowExecution(
                execution.id(),
                execution.workflowId(),
                execution.workflowVersionId(),
                WorkflowExecutionStatus.FAILED,
                execution.input(),
                Map.of("confirmRejected", true, "confirmTaskId", taskId),
                "Confirm rejected by user",
                execution.context(),
                null,
                execution.startedAt(),
                Instant.now()
        );
        executionStore.saveWorkflowExecution(rejected);
        return new WorkflowExecutionResult(rejected, executionStore.listNodeExecutions(executionId));
    }

    private WorkflowExecutionResult runFromNode(
            WorkflowExecutionRequest request,
            WorkflowVersion version,
            WorkflowDefinition definition,
            WorkflowNode currentNode,
            Map<String, Object> initialContext,
            WorkflowExecution existingExecution
    ) {
        Map<String, WorkflowNode> nodesById = definition.nodes().stream()
                .collect(Collectors.toMap(WorkflowNode::id, Function.identity()));
        Map<String, List<WorkflowEdge>> outgoingEdges = definition.edges().stream()
                .collect(Collectors.groupingBy(WorkflowEdge::sourceNodeId));

        Instant startedAt = existingExecution == null ? Instant.now() : existingExecution.startedAt();
        String executionId = existingExecution == null ? UUID.randomUUID().toString() : existingExecution.id();
        WorkflowExecution running = new WorkflowExecution(
                executionId,
                request.workflowId(),
                version.id(),
                WorkflowExecutionStatus.RUNNING,
                existingExecution == null ? request.input() : existingExecution.input(),
                Map.of(),
                null,
                initialContext,
                currentNode.id(),
                startedAt,
                null
        );
        executionStore.saveWorkflowExecution(running);

        Map<String, Object> context = new LinkedHashMap<>(initialContext);
        mergeGrantContext(context, request.systemVariables());
        context.put("系统变量", systemVariables(request.systemVariables()));
        context.put("__workflowExecutionId", executionId);
        Map<String, Object> finalOutput = Map.of();
        try {
            while (currentNode != null) {
                context.put("__currentNodeId", currentNode.id());
                NodeExecutionResult nodeResult = executeNode(executionId, currentNode, request.input(), context);
                mergeNodeOutput(context, currentNode, nodeResult.output());
                if (currentNode.type() == WorkflowNodeType.END) {
                    finalOutput = nodeResult.output();
                    break;
                }
                currentNode = nextNode(currentNode, nodeResult, outgoingEdges, nodesById, context);
            }

            WorkflowExecution succeeded = new WorkflowExecution(
                    executionId,
                    request.workflowId(),
                    version.id(),
                    WorkflowExecutionStatus.SUCCEEDED,
                    running.input(),
                    finalOutput,
                    null,
                    context,
                    null,
                    startedAt,
                    Instant.now()
            );
            executionStore.saveWorkflowExecution(succeeded);
            return new WorkflowExecutionResult(succeeded, executionStore.listNodeExecutions(executionId));
        } catch (ConfirmRequiredException ex) {
            Map<String, Object> waitingOutput = new LinkedHashMap<>();
            waitingOutput.put("confirmTaskId", ex.taskId());
            waitingOutput.put("confirmSummary", ex.summary());
            waitingOutput.put("connectorCode", ex.connectorCode());
            waitingOutput.put("operationCode", ex.operationCode());
            WorkflowExecution waiting = new WorkflowExecution(
                    executionId,
                    request.workflowId(),
                    version.id(),
                    WorkflowExecutionStatus.WAITING_CONFIRM,
                    running.input(),
                    waitingOutput,
                    null,
                    context,
                    ex.nodeId(),
                    startedAt,
                    null
            );
            executionStore.saveWorkflowExecution(waiting);
            return new WorkflowExecutionResult(waiting, executionStore.listNodeExecutions(executionId));
        } catch (RuntimeException ex) {
            WorkflowExecution failed = new WorkflowExecution(
                    executionId,
                    request.workflowId(),
                    version.id(),
                    WorkflowExecutionStatus.FAILED,
                    running.input(),
                    Map.of(),
                    ex.getMessage(),
                    context,
                    null,
                    startedAt,
                    Instant.now()
            );
            executionStore.saveWorkflowExecution(failed);
            return new WorkflowExecutionResult(failed, executionStore.listNodeExecutions(executionId));
        }
    }

    public WorkflowExecutionResult getWorkflowExecution(String executionId) {
        WorkflowExecution execution = executionStore.findWorkflowExecutionById(executionId)
                .orElseThrow(() -> new WorkflowRunNotFoundException(executionId));
        workflowService.getWorkflow(execution.workflowId());
        return new WorkflowExecutionResult(execution, executionStore.listNodeExecutions(executionId));
    }

    public List<WorkflowExecutionResult> listWorkflowExecutions() {
        String tenantId = tenantGuard == null ? TenantContext.requireTenantId() : tenantGuard.currentTenantId();
        return executionStore.listWorkflowExecutions(tenantId).stream()
                .map(execution -> new WorkflowExecutionResult(
                        execution,
                        executionStore.listNodeExecutions(execution.id())
                ))
                .toList();
    }

    public List<WorkflowExecution> listWorkflowExecutionHeaders() {
        String tenantId = tenantGuard == null ? TenantContext.requireTenantId() : tenantGuard.currentTenantId();
        return executionStore.listWorkflowExecutions(tenantId);
    }

    private NodeExecutionResult executeNode(
            String executionId,
            WorkflowNode node,
            Map<String, Object> input,
            Map<String, Object> context
    ) {
        Instant startedAt = Instant.now();
        Map<String, Object> nodeInput = new HashMap<>(context);
        try {
            WorkflowNodeExecutor executor = executorRegistry.getExecutor(node.type());
            NodeExecutionResult result = executeWithExceptionHandling(executor, node, new NodeExecutionContext(input, context));
            executionStore.saveNodeExecution(new NodeExecution(
                    UUID.randomUUID().toString(),
                    executionId,
                    node.id(),
                    node.type(),
                    NodeExecutionStatus.SUCCEEDED,
                    nodeInput,
                    result.output(),
                    null,
                    startedAt,
                    Instant.now()
            ));
            return result;
        } catch (RuntimeException ex) {
            executionStore.saveNodeExecution(new NodeExecution(
                    UUID.randomUUID().toString(),
                    executionId,
                    node.id(),
                    node.type(),
                    NodeExecutionStatus.FAILED,
                    nodeInput,
                    Map.of(),
                    ex.getMessage(),
                    startedAt,
                    Instant.now()
            ));
            throw ex;
        }
    }

    private NodeExecutionResult executeWithExceptionHandling(
            WorkflowNodeExecutor executor,
            WorkflowNode node,
            NodeExecutionContext context
    ) {
        if (!supportsExceptionHandling(node)) {
            return executor.execute(node, context);
        }
        int retryCount = intConfig(node, "retryCount", 0);
        RuntimeException lastException = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                return executeWithTimeout(executor, node, context);
            } catch (RuntimeException ex) {
                lastException = ex;
            }
        }
        if ("INTERRUPT_NODE".equals(stringConfig(node, "errorStrategy", "INTERRUPT_NODE"))) {
            throw lastException == null ? new IllegalStateException("Node execution failed.") : lastException;
        }
        throw lastException == null ? new IllegalStateException("Node execution failed.") : lastException;
    }

    private NodeExecutionResult executeWithTimeout(
            WorkflowNodeExecutor executor,
            WorkflowNode node,
            NodeExecutionContext context
    ) {
        int timeoutSeconds = intConfig(node, "timeoutSeconds", 60);
        Optional<WorkflowStreamSink> streamSink = WorkflowStreamContext.current();
        if (streamSink.isPresent()) {
            return executor.execute(node, context);
        }
        if (timeoutSeconds <= 0) {
            return executor.execute(node, context);
        }
        CompletableFuture<NodeExecutionResult> future = CompletableFuture.supplyAsync(() -> executor.execute(node, context));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new IllegalArgumentException("Node execution timed out after " + timeoutSeconds + " seconds.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Node execution interrupted.", ex);
        } catch (java.util.concurrent.ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof CompletionException completionException && completionException.getCause() != null) {
                cause = completionException.getCause();
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalArgumentException("Node execution failed.", cause);
        }
    }

    private boolean supportsExceptionHandling(WorkflowNode node) {
        return node.type() == WorkflowNodeType.KNOWLEDGE_RETRIEVAL || node.type() == WorkflowNodeType.LLM;
    }

    private WorkflowNode findStartNode(WorkflowDefinition definition) {
        return definition.nodes().stream()
                .filter(node -> node.type() == WorkflowNodeType.START)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Workflow definition has no START node."));
    }

    private WorkflowNode nextNode(
            WorkflowNode currentNode,
            NodeExecutionResult nodeResult,
            Map<String, List<WorkflowEdge>> outgoingEdges,
            Map<String, WorkflowNode> nodesById,
            Map<String, Object> context
    ) {
        String nextNodeId = nodeResult.nextNodeId()
                .orElseGet(() -> {
                    WorkflowEdge matchedEdge = selectMatchingEdge(
                            outgoingEdges.getOrDefault(currentNode.id(), List.of()),
                            context
                    );
                    return matchedEdge == null ? null : matchedEdge.targetNodeId();
                });
        if (nextNodeId == null) {
            return null;
        }
        WorkflowNode nextNode = nodesById.get(nextNodeId);
        if (nextNode == null) {
            throw new IllegalStateException("Workflow node not found: " + nextNodeId);
        }
        return nextNode;
    }

    private void mergeNodeOutput(Map<String, Object> context, WorkflowNode node, Map<String, Object> output) {
        context.putAll(output);
        context.put(node.id(), output);
        if (node.name() != null && !node.name().isBlank()) {
            context.put(node.name(), output);
        }
    }

    private void mergeGrantContext(Map<String, Object> context, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        copyIfPresent(variables, context, "userId");
        copyIfPresent(variables, context, "tenantId");
        copyIfPresent(variables, context, "userToken");
        copyIfPresent(variables, context, "botId");
        copyIfPresent(variables, context, "__traceId");
        copyIfPresent(variables, context, "__conversationId");
        copyIfPresent(variables, context, "activeUnitId");
        copyIfPresent(variables, context, "unitIds");
        copyIfPresent(variables, context, "departmentIds");
        Object activeUnitId = variables.get("activeUnitId");
        if (activeUnitId != null && !String.valueOf(activeUnitId).isBlank()) {
            context.putIfAbsent("unitId", activeUnitId);
        }
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key) && source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }

    private Map<String, Object> systemVariables(Map<String, Object> requestVariables) {
        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("datetime", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone).format(now));
        variables.put("date", LocalDate.now(zone).toString());
        variables.put("time", LocalTime.now(zone).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        variables.put("timestamp", now.toEpochMilli());
        variables.put("userId", stringValue(requestVariables.get("userId")));
        return variables;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int intConfig(WorkflowNode node, String key, int defaultValue) {
        Object value = node.config().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Integer.parseInt(stringValue);
        }
        return defaultValue;
    }

    private String stringConfig(WorkflowNode node, String key, String defaultValue) {
        Object value = node.config().get(key);
        return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : defaultValue;
    }

    private WorkflowEdge selectMatchingEdge(List<WorkflowEdge> edges, Map<String, Object> context) {
        if (edges.isEmpty()) {
            return null;
        }
        for (WorkflowEdge edge : edges) {
            if (containsEqualityCondition(edge.condition()) && matchesCondition(edge.condition(), context)) {
                return edge;
            }
        }
        for (WorkflowEdge edge : edges) {
            if (edge.condition() != null
                    && !edge.condition().isBlank()
                    && !containsEqualityCondition(edge.condition())
                    && matchesCondition(edge.condition(), context)) {
                return edge;
            }
        }
        for (WorkflowEdge edge : edges) {
            if (edge.condition() == null || edge.condition().isBlank()) {
                return edge;
            }
        }
        return null;
    }

    private boolean containsEqualityCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        return condition.matches(".*(?<![!=])==(?!=).*");
    }

    private boolean matchesCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        String expression = condition.trim();
        if (expression.contains("||")) {
            for (String part : expression.split("\\|\\|")) {
                if (matchesCondition(part.trim(), context)) {
                    return true;
                }
            }
            return false;
        }
        if (expression.contains("&&")) {
            for (String part : expression.split("&&")) {
                if (!matchesCondition(part.trim(), context)) {
                    return false;
                }
            }
            return true;
        }
        if (expression.contains("!=")) {
            String[] parts = expression.split("!=", 2);
            return !resolveConditionValue(parts[0], context).equals(cleanExpectedValue(parts[1]));
        }
        if (expression.contains("==")) {
            String[] parts = expression.split("==", 2);
            return resolveConditionValue(parts[0], context).equals(cleanExpectedValue(parts[1]));
        }
        Object value = TemplateRenderer.resolvePath(context, expression);
        return value instanceof Boolean booleanValue ? booleanValue : value != null && !String.valueOf(value).isBlank();
    }

    private String resolveConditionValue(String key, Map<String, Object> context) {
        Object value = TemplateRenderer.resolvePath(context, key.trim());
        return value == null ? "" : String.valueOf(value);
    }

    private String cleanExpectedValue(String value) {
        String expected = value.trim();
        if ((expected.startsWith("\"") && expected.endsWith("\"")) || (expected.startsWith("'") && expected.endsWith("'"))) {
            return expected.substring(1, expected.length() - 1);
        }
        return expected;
    }
}
