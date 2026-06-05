package com.mw.ai.agi.workflow.engine;

public class WorkflowRunNotFoundException extends RuntimeException {
    public WorkflowRunNotFoundException(String executionId) {
        super("Workflow run not found: " + executionId);
    }
}
