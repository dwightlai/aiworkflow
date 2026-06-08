package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;

public record KnowledgeBase(
        String id,
        String name,
        String description,
        String ownerUnitId,
        String embeddingModelId,
        String vectorStoreConfigId,
        int vectorDimension,
        String splitterType,
        int chunkSize,
        int chunkOverlap,
        String retrievalMode,
        int topK,
        String status,
        int documentCount,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt
) {
}
