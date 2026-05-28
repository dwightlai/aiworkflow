package com.aiworkflow.workflow.service;

public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(String workflowId) {
        super("Workflow not found: " + workflowId);
    }
}
