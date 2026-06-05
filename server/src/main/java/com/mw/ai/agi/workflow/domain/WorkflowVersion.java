package com.mw.ai.agi.workflow.domain;

import java.time.Instant;

public record WorkflowVersion(
        String id,
        String workflowId,
        int version,
        WorkflowDefinition definition,
        WorkflowVersionStatus status,
        String publishedBy,
        Instant publishedAt,
        Instant createdAt
) {
}
