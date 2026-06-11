package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowStatus;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;

import java.time.Instant;

public record WorkflowResponse(
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
        Instant updatedAt,
        WorkflowVersionResponse latestVersion
) {
    public static WorkflowResponse from(Workflow workflow, WorkflowVersion latestVersion) {
        return new WorkflowResponse(
                workflow.id(),
                workflow.tenantId(),
                workflow.ownerUnitId(),
                workflow.name(),
                workflow.description(),
                workflow.status(),
                workflow.currentVersionId(),
                workflow.createdBy(),
                workflow.updatedBy(),
                workflow.createdAt(),
                workflow.updatedAt(),
                latestVersion == null ? null : WorkflowVersionResponse.from(latestVersion)
        );
    }
}
