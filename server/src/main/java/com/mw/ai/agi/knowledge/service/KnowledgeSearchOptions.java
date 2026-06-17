package com.mw.ai.agi.knowledge.service;

import java.util.Map;

public record KnowledgeSearchOptions(
        String retrievalMode,
        KnowledgeRetrievalFilters filters
) {
    public static KnowledgeSearchOptions empty() {
        return new KnowledgeSearchOptions(null, KnowledgeRetrievalFilters.empty());
    }

    public static KnowledgeSearchOptions of(String retrievalMode, Map<String, Object> filters) {
        return new KnowledgeSearchOptions(retrievalMode, KnowledgeRetrievalFilters.from(filters));
    }
}
