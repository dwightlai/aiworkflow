package com.mw.ai.agi.openapi.client.model.bot;

import java.util.List;

public record OpenBotView(
        String id,
        String name,
        String description,
        String avatar,
        String workflowId,
        List<String> knowledgeBaseIds,
        String openingMessage,
        String status
) {
}
