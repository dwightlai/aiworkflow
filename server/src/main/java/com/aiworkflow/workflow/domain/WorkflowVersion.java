package com.aiworkflow.workflow.domain;

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
