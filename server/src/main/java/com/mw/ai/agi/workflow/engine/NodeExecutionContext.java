package com.mw.ai.agi.workflow.engine;

import java.util.Map;

public record NodeExecutionContext(
        Map<String, Object> input,
        Map<String, Object> context
) {
    public NodeExecutionContext {
        input = input == null ? Map.of() : Map.copyOf(input);
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
