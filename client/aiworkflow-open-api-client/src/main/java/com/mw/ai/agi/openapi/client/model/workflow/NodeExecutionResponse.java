package com.mw.ai.agi.openapi.client.model.workflow;

import java.time.Instant;
import java.util.Map;

public record NodeExecutionResponse(
        String id,
        String workflowExecutionId,
        String nodeId,
        String nodeType,
        String status,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
