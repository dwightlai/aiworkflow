package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;

public interface WorkflowNodeExecutor {
    WorkflowNodeType nodeType();

    NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context);
}
