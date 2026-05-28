package com.aiworkflow.workflow.api;

import com.aiworkflow.workflow.domain.WorkflowDefinition;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkflowDraftRequest(
        @NotNull WorkflowDefinition definition
) {
}
