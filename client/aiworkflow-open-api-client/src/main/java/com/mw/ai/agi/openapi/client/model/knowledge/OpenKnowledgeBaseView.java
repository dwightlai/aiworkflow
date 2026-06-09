package com.mw.ai.agi.openapi.client.model.knowledge;

public record OpenKnowledgeBaseView(
        String id,
        String name,
        String description,
        String retrievalMode,
        int topK
) {
}
