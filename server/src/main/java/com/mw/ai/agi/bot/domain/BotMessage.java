package com.mw.ai.agi.bot.domain;

import java.time.Instant;
import java.util.Map;

public record BotMessage(
        String id,
        String sessionId,
        String botId,
        BotMessageRole role,
        String content,
        Instant createdAt,
        Map<String, Object> metadata,
        String messageType
) {
    public BotMessage {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (messageType == null || messageType.isBlank()) {
            messageType = "TEXT";
        }
    }

    public BotMessage(String id, String sessionId, String botId, BotMessageRole role, String content, Instant createdAt) {
        this(id, sessionId, botId, role, content, createdAt, Map.of(), "TEXT");
    }
}
