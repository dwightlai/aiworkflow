package com.mw.ai.agi.workflow.engine;

import java.util.Map;

public record WorkflowExecutionRequest(
        String workflowId,
        Map<String, Object> input,
        Map<String, Object> systemVariables
) {
    public WorkflowExecutionRequest {
        input = input == null ? Map.of() : Map.copyOf(input);
        systemVariables = systemVariables == null ? Map.of() : Map.copyOf(systemVariables);
    }

    public WorkflowExecutionRequest(String workflowId, Map<String, Object> input) {
        this(workflowId, input, Map.of());
    }
}
