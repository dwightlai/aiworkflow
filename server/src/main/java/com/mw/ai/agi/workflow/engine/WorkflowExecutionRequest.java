package com.mw.ai.agi.workflow.engine;

import java.util.Map;

public record WorkflowExecutionRequest(
        String workflowId,
        Map<String, Object> input
) {
    public WorkflowExecutionRequest {
        input = input == null ? Map.of() : Map.copyOf(input);
    }
}
