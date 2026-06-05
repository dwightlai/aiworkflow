package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkflowDraftRequest(
        @NotNull WorkflowDefinition definition
) {
}
