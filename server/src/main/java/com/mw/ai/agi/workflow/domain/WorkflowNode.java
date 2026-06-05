package com.mw.ai.agi.workflow.domain;

import java.util.Map;

public record WorkflowNode(
        String id,
        WorkflowNodeType type,
        String name,
        Map<String, Object> config
) {
    public WorkflowNode {
        config = config == null ? Map.of() : Map.copyOf(config);
    }
}
