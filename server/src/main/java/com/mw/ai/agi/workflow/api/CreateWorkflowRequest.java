package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkflowRequest(
        @NotBlank String name,
        String description,
        @NotNull WorkflowDefinition definition
) {
}
