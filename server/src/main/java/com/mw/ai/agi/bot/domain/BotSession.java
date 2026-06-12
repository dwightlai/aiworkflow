package com.mw.ai.agi.bot.domain;

import java.time.Instant;

public record BotSession(
        String id,
        String botId,
        String title,
        int messageCount,
        Instant createdAt,
        Instant updatedAt,
        boolean pinned,
        String userId
) {
    public BotSession(
            String id,
            String botId,
            String title,
            int messageCount,
            Instant createdAt,
            Instant updatedAt,
            boolean pinned
    ) {
        this(id, botId, title, messageCount, createdAt, updatedAt, pinned, null);
    }
}
