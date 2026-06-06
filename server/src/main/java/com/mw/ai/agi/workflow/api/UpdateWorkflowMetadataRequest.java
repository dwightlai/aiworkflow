package com.mw.ai.agi.workflow.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkflowMetadataRequest(
        @NotBlank
        @Size(max = 200)
        String name,
        String description
) {
}
