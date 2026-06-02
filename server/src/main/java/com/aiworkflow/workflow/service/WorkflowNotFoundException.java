package com.aiworkflow.workflow.service;

public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(String workflowId) {
        super("Workflow not found: " + workflowId);
    }

    private WorkflowNotFoundException(String message, boolean rawMessage) {
        super(message);
    }

    public static WorkflowNotFoundException publishedVersionNotFound(String workflowId) {
        return new WorkflowNotFoundException("Published version not found for workflow: " + workflowId, true);
    }
}
