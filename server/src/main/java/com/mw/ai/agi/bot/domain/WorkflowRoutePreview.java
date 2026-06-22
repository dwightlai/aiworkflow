package com.mw.ai.agi.bot.domain;

public record WorkflowRoutePreview(
        String workflowId,
        String workflowName,
        String capabilityCode,
        String matchReason
) {
}
