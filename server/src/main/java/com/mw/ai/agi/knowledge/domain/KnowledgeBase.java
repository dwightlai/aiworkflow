package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;

public record KnowledgeBase(
        String id,
        String tenantId,
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
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt,
        String kbType,
        String bizScope,
        String datasetMode,
        String defaultDatasetId,
        int datasetCount,
        String metadataJson
) {
    public KnowledgeBase(
            String id,
            String tenantId,
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
            String createdBy,
            String updatedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id, tenantId, name, description, ownerUnitId, embeddingModelId, vectorStoreConfigId,
                vectorDimension, splitterType, chunkSize, chunkOverlap, retrievalMode, topK, status,
                documentCount, chunkCount, createdBy, updatedBy, createdAt, updatedAt,
                "NORMAL", null, "MULTI", null, 0, null
        );
    }

    public boolean isMultiDataset() {
        return "MULTI".equalsIgnoreCase(datasetMode);
    }

    public boolean isArchiveTopic() {
        return "ARCHIVE_TOPIC".equalsIgnoreCase(kbType) || "ARCHIVE_TOPIC_COMPILE".equalsIgnoreCase(kbType);
    }
}
