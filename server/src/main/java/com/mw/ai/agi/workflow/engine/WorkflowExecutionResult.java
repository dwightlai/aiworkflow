package com.mw.ai.agi.workflow.engine;

import java.util.List;

public record WorkflowExecutionResult(
        WorkflowExecution execution,
        List<NodeExecution> nodeExecutions
) {
    public WorkflowExecutionResult {
        nodeExecutions = nodeExecutions == null ? List.of() : List.copyOf(nodeExecutions);
    }
}
