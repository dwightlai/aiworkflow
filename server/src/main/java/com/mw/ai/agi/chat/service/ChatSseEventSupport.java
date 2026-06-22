package com.mw.ai.agi.chat.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.mw.ai.agi.workflow.engine.NodeExecutionStatus;
import com.mw.ai.agi.workflow.engine.NodeExecution;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChatSseEventSupport {
    private ChatSseEventSupport() {
    }

    public static List<Map<String, Object>> extractCitations(WorkflowExecutionResult execution) {
        if (execution == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> citations = new ArrayList<>();
        appendCitationMaps(citations, seen, execution.execution().output().get("sources"));
        appendCitationMaps(citations, seen, execution.execution().output().get("documents"));
        for (NodeExecution nodeExecution : execution.nodeExecutions()) {
            if (nodeExecution.nodeType() != WorkflowNodeType.KNOWLEDGE_RETRIEVAL) {
                continue;
            }
            appendCitationMaps(citations, seen, nodeExecution.output().get("sources"));
        }
        return citations;
    }

    public static void emitWorkflowEvents(SseEmitter emitter, WorkflowExecutionResult execution) throws IOException {
        emitWorkflowEvents(emitter, execution, true, false);
    }

    public static void emitWorkflowEvents(
            SseEmitter emitter,
            WorkflowExecutionResult execution,
            boolean includeCitations
    ) throws IOException {
        emitWorkflowEvents(emitter, execution, includeCitations, false);
    }

    public static void emitWorkflowEvents(
            SseEmitter emitter,
            WorkflowExecutionResult execution,
            boolean includeCitations,
            boolean skipToolEvents
    ) throws IOException {
        if (execution == null) {
            return;
        }
        if (!skipToolEvents) {
            emitToolEvents(emitter, execution);
        }
        if (includeCitations) {
            for (Map<String, Object> citation : extractCitations(execution)) {
                emitter.send(SseEmitter.event().name("citation.added").data(citation));
            }
        }
        emitAgentJobEvents(emitter, execution.execution().output());
    }

    private static void emitAgentJobEvents(SseEmitter emitter, Map<String, Object> output) throws IOException {
        if (output == null || output.isEmpty()) {
            return;
        }
        String jobId = firstNonBlank(
                stringValue(output.get("agentJobId")),
                stringValue(output.get("jobId"))
        );
        if (jobId.isBlank()) {
            return;
        }
        emitter.send(SseEmitter.event().name("job.started").data(Map.of("jobId", jobId)));
        String status = stringValue(output.get("jobStatus"));
        int progress = parseProgress(output.get("progress"));
        String currentStep = stringValue(output.get("currentStep"));
        if ("RUNNING".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)
                || (progress > 0 && progress < 100)) {
            Map<String, Object> progressData = new LinkedHashMap<>();
            progressData.put("jobId", jobId);
            progressData.put("progress", progress);
            if (!currentStep.isBlank()) {
                progressData.put("currentStep", currentStep);
            }
            emitter.send(SseEmitter.event().name("job.progress").data(progressData));
            return;
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            emitter.send(SseEmitter.event().name("error").data(Map.of(
                    "message", stringValue(output.get("errorMessage")).isBlank() ? "任务执行失败" : stringValue(output.get("errorMessage"))
            )));
            return;
        }
        if (!"COMPLETED".equalsIgnoreCase(status) && !"SUCCEEDED".equalsIgnoreCase(status) && progress < 100) {
            return;
        }
        Map<String, Object> completed = new LinkedHashMap<>();
        completed.put("jobId", jobId);
        String title = stringValue(output.get("title"));
        if (!title.isBlank()) {
            completed.put("title", title);
        }
        String outputId = firstNonBlank(stringValue(output.get("outputId")), stringValue(output.get("resultId")));
        if (!outputId.isBlank()) {
            completed.put("outputId", outputId);
        }
        String downloadUrl = stringValue(output.get("downloadUrl"));
        if (!downloadUrl.isBlank()) {
            completed.put("downloadUrl", downloadUrl);
        }
        String resultUrl = stringValue(output.get("resultUrl"));
        if (!resultUrl.isBlank()) {
            completed.put("resultUrl", resultUrl);
        }
        emitter.send(SseEmitter.event().name("job.completed").data(completed));
    }

    private static void emitToolEvents(SseEmitter emitter, WorkflowExecutionResult execution) throws IOException {
        for (NodeExecution nodeExecution : execution.nodeExecutions()) {
            if (nodeExecution.nodeType() != WorkflowNodeType.HTTP_TOOL) {
                continue;
            }
            Map<String, Object> input = nodeExecution.input();
            String connectorCode = stringValue(input.get("connectorCode"));
            String operationCode = stringValue(input.get("operationCode"));
            if (connectorCode.isBlank() && operationCode.isBlank()) {
                continue;
            }
            String eventName = nodeExecution.status() == NodeExecutionStatus.FAILED ? "tool.failed" : "tool.completed";
            emitter.send(SseEmitter.event().name("tool.started").data(Map.of(
                    "connectorCode", connectorCode,
                    "operationCode", operationCode,
                    "name", operationCode.isBlank() ? connectorCode : operationCode
            )));
            emitter.send(SseEmitter.event().name(eventName).data(Map.of(
                    "connectorCode", connectorCode,
                    "operationCode", operationCode,
                    "name", operationCode.isBlank() ? connectorCode : operationCode
            )));
        }
    }

    public static void emitConfirmRequired(
            SseEmitter emitter,
            String taskId,
            String summary,
            Map<String, Object> payloadSnapshot
    ) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("summary", summary);
        if (payloadSnapshot != null && !payloadSnapshot.isEmpty()) {
            data.put("payloadSnapshot", payloadSnapshot);
        }
        emitter.send(SseEmitter.event().name("confirm.required").data(data));
    }

    private static void appendCitationMaps(List<Map<String, Object>> citations, Set<String> seen, Object value) {
        if (!(value instanceof List<?> items)) {
            return;
        }
        for (Object item : items) {
            Map<String, Object> citation = toCitation(item);
            if (citation.isEmpty()) {
                continue;
            }
            String key = stringValue(citation.get("title"));
            if (!seen.add(key)) {
                continue;
            }
            citations.add(citation);
        }
    }

    private static Map<String, Object> toCitation(Object item) {
        if (item instanceof KnowledgeSearchResult result) {
            Map<String, Object> citation = new LinkedHashMap<>();
            citation.put("sourceId", result.id());
            citation.put("title", result.documentName());
            citation.put("excerpt", truncate(result.content(), 240));
            citation.put("sourceType", "KNOWLEDGE");
            citation.put("score", result.score());
            return citation;
        }
        if (item instanceof Map<?, ?> map) {
            Map<String, Object> citation = new LinkedHashMap<>();
            Object id = map.get("id");
            Object documentName = map.get("documentName");
            Object content = map.get("content");
            if (id == null && documentName == null && content == null) {
                return Map.of();
            }
            citation.put("sourceId", id == null ? "" : String.valueOf(id));
            citation.put("title", documentName == null ? String.valueOf(id) : String.valueOf(documentName));
            if (content != null) {
                citation.put("excerpt", truncate(String.valueOf(content), 240));
            }
            citation.put("sourceType", "KNOWLEDGE");
            Object score = map.get("score");
            if (score != null) {
                citation.put("score", score);
            }
            return citation;
        }
        return Map.of();
    }

    private static int parseProgress(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String firstNonBlank(String left, String right) {
        if (left != null && !left.isBlank()) {
            return left;
        }
        return right == null ? "" : right;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}
