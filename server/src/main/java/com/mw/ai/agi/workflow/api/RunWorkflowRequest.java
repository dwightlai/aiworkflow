package com.mw.ai.agi.workflow.api;

import java.util.Map;

public record RunWorkflowRequest(Map<String, Object> input) {
    public RunWorkflowRequest {
        input = input == null ? Map.of() : Map.copyOf(input);
    }
}
