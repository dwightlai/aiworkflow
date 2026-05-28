package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNodeType;

import java.time.Instant;
import java.util.Map;

public record NodeExecution(
        String id,
        String workflowExecutionId,
        String nodeId,
        WorkflowNodeType nodeType,
        NodeExecutionStatus status,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
    public NodeExecution {
        input = input == null ? Map.of() : Map.copyOf(input);
        output = output == null ? Map.of() : Map.copyOf(output);
    }
}
