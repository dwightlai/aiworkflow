package com.mw.ai.agi.openapi.client.model.bot;

import java.util.List;

public record OpenBotView(
        String id,
        String name,
        String description,
        String avatar,
        String workflowId,
        String modelProviderId,
        List<String> knowledgeBaseIds,
        String openingMessage,
        String capabilityHint,
        List<String> suggestedQuestions,
        String status
) {
}
