package com.aiworkflow.workflow.engine;

import java.util.List;
import java.util.Optional;

public interface WorkflowExecutionStore {
    WorkflowExecution saveWorkflowExecution(WorkflowExecution execution);

    Optional<WorkflowExecution> findWorkflowExecutionById(String executionId);

    List<WorkflowExecution> listWorkflowExecutions();

    NodeExecution saveNodeExecution(NodeExecution nodeExecution);

    List<NodeExecution> listNodeExecutions(String workflowExecutionId);
}
