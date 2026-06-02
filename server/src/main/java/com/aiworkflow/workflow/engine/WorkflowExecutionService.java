package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowEdge;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.service.WorkflowApplicationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowExecutionService {
    private final WorkflowApplicationService workflowService;
    private final WorkflowExecutionStore executionStore;
    private final WorkflowNodeExecutorRegistry executorRegistry;

    public WorkflowExecutionService(
            WorkflowApplicationService workflowService,
            WorkflowExecutionStore executionStore,
            WorkflowNodeExecutorRegistry executorRegistry
    ) {
        this.workflowService = workflowService;
        this.executionStore = executionStore;
        this.executorRegistry = executorRegistry;
    }

    public WorkflowExecutionResult runWorkflow(WorkflowExecutionRequest request) {
        WorkflowVersion version = workflowService.getPublishedVersion(request.workflowId());
        WorkflowDefinition definition = version.definition();
        WorkflowNode currentNode = findStartNode(definition);
        Map<String, WorkflowNode> nodesById = definition.nodes().stream()
                .collect(Collectors.toMap(WorkflowNode::id, Function.identity()));
        Map<String, List<WorkflowEdge>> outgoingEdges = definition.edges().stream()
                .collect(Collectors.groupingBy(WorkflowEdge::sourceNodeId));

        Instant startedAt = Instant.now();
        String executionId = UUID.randomUUID().toString();
        WorkflowExecution execution = new WorkflowExecution(
                executionId,
                request.workflowId(),
                version.id(),
                WorkflowExecutionStatus.RUNNING,
                request.input(),
                Map.of(),
                null,
                startedAt,
                null
        );
        executionStore.saveWorkflowExecution(execution);

        Map<String, Object> context = new LinkedHashMap<>(request.input());
        Map<String, Object> finalOutput = Map.of();
        try {
            while (currentNode != null) {
                NodeExecutionResult nodeResult = executeNode(executionId, currentNode, request.input(), context);
                context.putAll(nodeResult.output());
                if (currentNode.type() == WorkflowNodeType.END) {
                    finalOutput = nodeResult.output();
                    break;
                }
                currentNode = nextNode(currentNode, nodeResult, outgoingEdges, nodesById);
            }

            WorkflowExecution succeeded = new WorkflowExecution(
                    execution.id(),
                    execution.workflowId(),
                    execution.workflowVersionId(),
                    WorkflowExecutionStatus.SUCCEEDED,
                    execution.input(),
                    finalOutput,
                    null,
                    execution.startedAt(),
                    Instant.now()
            );
            executionStore.saveWorkflowExecution(succeeded);
            return new WorkflowExecutionResult(succeeded, executionStore.listNodeExecutions(executionId));
        } catch (RuntimeException ex) {
            WorkflowExecution failed = new WorkflowExecution(
                    execution.id(),
                    execution.workflowId(),
                    execution.workflowVersionId(),
                    WorkflowExecutionStatus.FAILED,
                    execution.input(),
                    Map.of(),
                    ex.getMessage(),
                    execution.startedAt(),
                    Instant.now()
            );
            executionStore.saveWorkflowExecution(failed);
            return new WorkflowExecutionResult(failed, executionStore.listNodeExecutions(executionId));
        }
    }

    public WorkflowExecutionResult getWorkflowExecution(String executionId) {
        WorkflowExecution execution = executionStore.findWorkflowExecutionById(executionId)
                .orElseThrow(() -> new WorkflowRunNotFoundException(executionId));
        return new WorkflowExecutionResult(execution, executionStore.listNodeExecutions(executionId));
    }

    public List<WorkflowExecutionResult> listWorkflowExecutions() {
        return executionStore.listWorkflowExecutions().stream()
                .map(execution -> new WorkflowExecutionResult(
                        execution,
                        executionStore.listNodeExecutions(execution.id())
                ))
                .toList();
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
            NodeExecutionResult result = executor.execute(node, new NodeExecutionContext(input, context));
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
            Map<String, WorkflowNode> nodesById
    ) {
        String nextNodeId = nodeResult.nextNodeId()
                .orElseGet(() -> outgoingEdges.getOrDefault(currentNode.id(), List.of()).stream()
                        .findFirst()
                        .map(WorkflowEdge::targetNodeId)
                        .orElse(null));
        if (nextNodeId == null) {
            return null;
        }
        WorkflowNode nextNode = nodesById.get(nextNodeId);
        if (nextNode == null) {
            throw new IllegalStateException("Workflow node not found: " + nextNodeId);
        }
        return nextNode;
    }
}
