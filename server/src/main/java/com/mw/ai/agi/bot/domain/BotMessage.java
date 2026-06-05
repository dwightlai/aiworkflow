package com.mw.ai.agi.bot.domain;

import java.time.Instant;

public record BotMessage(
        String id,
        String sessionId,
        String botId,
        BotMessageRole role,
        String content,
        Instant createdAt
) {
}
