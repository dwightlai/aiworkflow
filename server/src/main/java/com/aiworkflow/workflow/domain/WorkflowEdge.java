package com.aiworkflow.workflow.domain;

public record WorkflowEdge(
        String id,
        String sourceNodeId,
        String targetNodeId,
        String condition
) {
}
