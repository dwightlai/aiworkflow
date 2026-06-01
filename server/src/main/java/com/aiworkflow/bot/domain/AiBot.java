package com.aiworkflow.bot.domain;

import java.time.Instant;

public record AiBot(
        String id,
        String name,
        String description,
        String avatar,
        String workflowId,
        String modelProviderId,
        String knowledgeBaseId,
        String systemPrompt,
        String openingMessage,
        BotStatus status,
        int conversationCount,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
