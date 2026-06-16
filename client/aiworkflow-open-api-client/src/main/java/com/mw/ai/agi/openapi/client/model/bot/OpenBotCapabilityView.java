package com.mw.ai.agi.openapi.client.model.bot;

public record OpenBotCapabilityView(
        String id,
        String capabilityType,
        String capabilityId,
        String capabilityCode,
        String routingKeywords,
        boolean primaryCapability,
        boolean enabled
) {
}
