package com.aiworkflow.knowledge.domain;

public record KnowledgeSearchResult(
        String id,
        String documentName,
        String content,
        int score
) {
}
