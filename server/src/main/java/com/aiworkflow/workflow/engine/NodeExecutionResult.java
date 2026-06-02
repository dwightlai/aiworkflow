package com.aiworkflow.workflow.engine;

import java.util.Map;
import java.util.Optional;

public record NodeExecutionResult(
        Map<String, Object> output,
        Optional<String> nextNodeId
) {
    public NodeExecutionResult {
        output = output == null ? Map.of() : Map.copyOf(output);
        nextNodeId = nextNodeId == null ? Optional.empty() : nextNodeId;
    }

    public static NodeExecutionResult output(Map<String, Object> output) {
        return new NodeExecutionResult(output, Optional.empty());
    }

    public static NodeExecutionResult branch(String nextNodeId) {
        return new NodeExecutionResult(Map.of(), Optional.ofNullable(nextNodeId));
    }
}
