package com.mw.ai.agi.workflow.domain;

import java.time.Instant;

public record Workflow(
        String id,
        String tenantId,
        String ownerUnitId,
        String name,
        String description,
        WorkflowStatus status,
        String currentVersionId,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
