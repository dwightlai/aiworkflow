package com.mw.ai.agi.workflow.domain;

public record WorkflowEdge(
        String id,
        String sourceNodeId,
        String targetNodeId,
        String condition
) {
}
