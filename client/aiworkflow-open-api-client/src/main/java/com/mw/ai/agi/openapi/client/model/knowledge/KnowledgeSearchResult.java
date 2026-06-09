package com.mw.ai.agi.openapi.client.model.knowledge;

public record KnowledgeSearchResult(
        String id,
        String documentName,
        String content,
        int score
) {
}
