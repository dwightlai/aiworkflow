package com.mw.ai.agi.openapi.client.model.workflow;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowExecutionResponse(
        String id,
        String workflowId,
        String workflowVersionId,
        String status,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        List<NodeExecutionResponse> nodeExecutions
) {
}
