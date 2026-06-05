package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.workflow.domain.WorkflowVersion;
import com.mw.ai.agi.workflow.domain.WorkflowVersionStatus;

import java.time.Instant;

public record WorkflowVersionResponse(
        String id,
        String workflowId,
        int version,
        WorkflowDefinitionResponse definition,
        WorkflowVersionStatus status,
        String publishedBy,
        Instant publishedAt,
        Instant createdAt
) {
    public static WorkflowVersionResponse from(WorkflowVersion version) {
        return new WorkflowVersionResponse(
                version.id(),
                version.workflowId(),
                version.version(),
                WorkflowDefinitionResponse.from(version.definition()),
                version.status(),
                version.publishedBy(),
                version.publishedAt(),
                version.createdAt()
        );
    }
}
