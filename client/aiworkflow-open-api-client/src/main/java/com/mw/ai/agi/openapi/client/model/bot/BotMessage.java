package com.mw.ai.agi.openapi.client.model.bot;

import java.time.Instant;

public record BotMessage(
        String id,
        String sessionId,
        String botId,
        String role,
        String content,
        Instant createdAt
) {
}
