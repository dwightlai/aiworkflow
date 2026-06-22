package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.workflow.engine.WorkflowExecution;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowExecutionResponse(
        String id,
        String workflowId,
        String workflowVersionId,
        WorkflowExecutionStatus status,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        List<NodeExecutionResponse> nodeExecutions
) {
    public static WorkflowExecutionResponse from(WorkflowExecutionResult result) {
        WorkflowExecution execution = result.execution();
        return new WorkflowExecutionResponse(
                execution.id(),
                execution.workflowId(),
                execution.workflowVersionId(),
                execution.status(),
                execution.input(),
                execution.output(),
                execution.errorMessage(),
                execution.startedAt(),
                execution.finishedAt(),
                result.nodeExecutions().stream()
                        .map(NodeExecutionResponse::from)
                        .toList()
        );
    }

    public static WorkflowExecutionResponse summaryFrom(WorkflowExecution execution) {
        return new WorkflowExecutionResponse(
                execution.id(),
                execution.workflowId(),
                execution.workflowVersionId(),
                execution.status(),
                Map.of(),
                Map.of(),
                execution.errorMessage(),
                execution.startedAt(),
                execution.finishedAt(),
                List.of()
        );
    }
}
