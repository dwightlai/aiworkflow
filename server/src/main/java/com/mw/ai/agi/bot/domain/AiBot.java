package com.mw.ai.agi.bot.domain;

import java.time.Instant;
import java.util.List;

public record AiBot(
        String id,
        String tenantId,
        String name,
        String description,
        String ownerUnitId,
        String avatar,
        String workflowId,
        String modelProviderId,
        List<String> knowledgeBaseIds,
        String systemPrompt,
        String openingMessage,
        String capabilityHint,
        List<String> suggestedQuestions,
        BotStatus status,
        int conversationCount,
        Instant publishedAt,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
