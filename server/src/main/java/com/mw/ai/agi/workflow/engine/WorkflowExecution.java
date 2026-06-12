package com.mw.ai.agi.workflow.engine;

import java.time.Instant;
import java.util.Map;

public record WorkflowExecution(
        String id,
        String workflowId,
        String workflowVersionId,
        WorkflowExecutionStatus status,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Map<String, Object> context,
        String currentNodeId,
        Instant startedAt,
        Instant finishedAt
) {
    public WorkflowExecution {
        input = input == null ? Map.of() : Map.copyOf(input);
        output = output == null ? Map.of() : Map.copyOf(output);
        context = context == null ? Map.of() : Map.copyOf(context);
    }

    public WorkflowExecution(
            String id,
            String workflowId,
            String workflowVersionId,
            WorkflowExecutionStatus status,
            Map<String, Object> input,
            Map<String, Object> output,
            String errorMessage,
            Instant startedAt,
            Instant finishedAt
    ) {
        this(id, workflowId, workflowVersionId, status, input, output, errorMessage, Map.of(), null, startedAt, finishedAt);
    }
}
