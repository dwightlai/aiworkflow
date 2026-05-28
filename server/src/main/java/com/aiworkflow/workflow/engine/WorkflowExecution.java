package com.aiworkflow.workflow.engine;

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
        Instant startedAt,
        Instant finishedAt
) {
    public WorkflowExecution {
        input = input == null ? Map.of() : Map.copyOf(input);
        output = output == null ? Map.of() : Map.copyOf(output);
    }
}
