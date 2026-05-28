package com.aiworkflow.workflow.api;

import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.domain.WorkflowVersionStatus;

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
