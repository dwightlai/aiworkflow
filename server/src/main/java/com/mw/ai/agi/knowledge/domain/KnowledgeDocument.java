package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;

public record KnowledgeDocument(
        String id,
        String knowledgeBaseId,
        String name,
        int chunkCount,
        Instant createdAt,
        String datasetType,
        String processingStatus,
        String tags,
        String category,
        String source,
        int rowCount,
        String parserType,
        String splitterType,
        String splitterConfig,
        String rawContent,
        String errorMessage
) {
    public KnowledgeDocument(String id, String knowledgeBaseId, String name, int chunkCount, Instant createdAt) {
        this(
                id,
                knowledgeBaseId,
                name,
                chunkCount,
                createdAt,
                "TEXT_DOCUMENT",
                "READY",
                null,
                null,
                null,
                0,
                "TEXT",
                "FIXED_LENGTH",
                "{}",
                null,
                null
        );
    }
}
