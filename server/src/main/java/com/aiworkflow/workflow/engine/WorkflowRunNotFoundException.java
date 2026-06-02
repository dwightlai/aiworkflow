package com.aiworkflow.workflow.engine;

public class WorkflowRunNotFoundException extends RuntimeException {
    public WorkflowRunNotFoundException(String executionId) {
        super("Workflow run not found: " + executionId);
    }
}
