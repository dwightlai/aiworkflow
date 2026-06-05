package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;

public interface WorkflowNodeExecutor {
    WorkflowNodeType nodeType();

    NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context);
}
