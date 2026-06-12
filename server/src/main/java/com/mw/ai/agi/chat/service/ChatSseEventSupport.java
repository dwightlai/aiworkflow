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
        emitWorkflowEvents(emitter, execution, true);
    }

    public static void emitWorkflowEvents(
            SseEmitter emitter,
            WorkflowExecutionResult execution,
            boolean includeCitations
    ) throws IOException {
        if (execution == null) {
            return;
        }
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
            emitter.send(SseEmitter.event().name(eventName).data(Map.of(
                    "connectorCode", connectorCode,
                    "operationCode", operationCode,
                    "name", operationCode.isBlank() ? connectorCode : operationCode
            )));
        }
        if (includeCitations) {
            for (Map<String, Object> citation : extractCitations(execution)) {
                emitter.send(SseEmitter.event().name("citation.added").data(citation));
            }
        }
        emitGenerationJobEvents(emitter, execution.execution().output());
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

    private static void emitGenerationJobEvents(SseEmitter emitter, Map<String, Object> output) throws IOException {
        if (output == null || output.isEmpty()) {
            return;
        }
        String jobId = firstNonBlank(
                stringValue(output.get("generationJobId")),
                stringValue(output.get("jobId"))
        );
        if (jobId.isBlank()) {
            return;
        }
        String outputId = stringValue(output.get("generationOutputId"));
        if (outputId.isBlank()) {
            outputId = stringValue(output.get("outputId"));
        }
        String status = stringValue(output.get("jobStatus"));
        int progress = parseProgress(output.get("progress"));
        if ("RUNNING".equalsIgnoreCase(status) || progress > 0 && progress < 100) {
            emitter.send(SseEmitter.event().name("job.progress").data(Map.of(
                    "jobId", jobId,
                    "progress", progress,
                    "currentStep", stringValue(output.get("currentStep"))
            )));
        }
        Map<String, Object> completed = new LinkedHashMap<>();
        completed.put("jobId", jobId);
        if (!outputId.isBlank()) {
            completed.put("outputId", outputId);
            completed.put("downloadUrl", "/api/research/outputs/" + outputId + "/docx");
        }
        completed.put("title", stringValue(output.get("title")));
        emitter.send(SseEmitter.event().name("job.completed").data(completed));
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
