package com.aiworkflow.workflow.domain;

import java.util.List;

public record WorkflowDefinition(
        List<WorkflowNode> nodes,
        List<WorkflowEdge> edges,
        List<WorkflowVariable> variables
) {
}
