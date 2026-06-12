package com.mw.ai.agi.bot.domain;

import java.time.Instant;

public record BotCapability(
        String id,
        String tenantId,
        String botId,
        String capabilityType,
        String capabilityId,
        String capabilityCode,
        String routingKeywords,
        boolean primaryCapability,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
