package com.aiworkflow.workflow.domain;

import java.time.Instant;

public record Workflow(
        String id,
        String tenantId,
        String name,
        String description,
        WorkflowStatus status,
        String currentVersionId,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
