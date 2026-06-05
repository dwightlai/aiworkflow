package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowEdge;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowVariable;

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
