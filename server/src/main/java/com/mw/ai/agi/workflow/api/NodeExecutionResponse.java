package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.mw.ai.agi.workflow.engine.NodeExecution;
import com.mw.ai.agi.workflow.engine.NodeExecutionStatus;

import java.time.Instant;
import java.util.Map;

public record NodeExecutionResponse(
        String id,
        String workflowExecutionId,
        String nodeId,
        WorkflowNodeType nodeType,
        NodeExecutionStatus status,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
    public static NodeExecutionResponse from(NodeExecution nodeExecution) {
        return new NodeExecutionResponse(
                nodeExecution.id(),
                nodeExecution.workflowExecutionId(),
                nodeExecution.nodeId(),
                nodeExecution.nodeType(),
                nodeExecution.status(),
                nodeExecution.input(),
                nodeExecution.output(),
                nodeExecution.errorMessage(),
                nodeExecution.startedAt(),
                nodeExecution.finishedAt()
        );
    }
}
