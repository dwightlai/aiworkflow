package com.aiworkflow.bot.domain;

import java.time.Instant;

public record BotSession(
        String id,
        String botId,
        String title,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
