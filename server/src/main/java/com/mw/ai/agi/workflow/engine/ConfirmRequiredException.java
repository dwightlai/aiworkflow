package com.mw.ai.agi.workflow.engine;

public class ConfirmRequiredException extends RuntimeException {
    private final String taskId;
    private final String workflowRunId;
    private final String nodeId;
    private final String summary;
    private final String connectorCode;
    private final String operationCode;

    public ConfirmRequiredException(
            String taskId,
            String workflowRunId,
            String nodeId,
            String summary,
            String connectorCode,
            String operationCode
    ) {
        super("Confirm required: " + summary);
        this.taskId = taskId;
        this.workflowRunId = workflowRunId;
        this.nodeId = nodeId;
        this.summary = summary;
        this.connectorCode = connectorCode;
        this.operationCode = operationCode;
    }

    public String taskId() {
        return taskId;
    }

    public String workflowRunId() {
        return workflowRunId;
    }

    public String nodeId() {
        return nodeId;
    }

    public String summary() {
        return summary;
    }

    public String connectorCode() {
        return connectorCode;
    }

    public String operationCode() {
        return operationCode;
    }
}
