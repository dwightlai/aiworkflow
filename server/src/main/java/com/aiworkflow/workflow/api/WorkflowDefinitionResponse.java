package com.aiworkflow.workflow.api;

import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowEdge;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowVariable;

import java.util.List;

public record WorkflowDefinitionResponse(
        List<WorkflowNode> nodes,
        List<WorkflowEdge> edges,
        List<WorkflowVariable> variables
) {
    public static WorkflowDefinitionResponse from(WorkflowDefinition definition) {
        return new WorkflowDefinitionResponse(definition.nodes(), definition.edges(), definition.variables());
    }
}
