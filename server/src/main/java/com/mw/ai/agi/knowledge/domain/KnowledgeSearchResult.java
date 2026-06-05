package com.mw.ai.agi.knowledge.domain;

public record KnowledgeSearchResult(
        String id,
        String documentName,
        String content,
        int score
) {
}
