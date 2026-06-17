package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;

public record KnowledgeDataset(
        String id,
        String tenantId,
        String knowledgeBaseId,
        String name,
        String code,
        String description,
        String datasetType,
        String bizType,
        String bizId,
        String topicId,
        String topicTitle,
        String securityLevel,
        String retentionPeriod,
        String ownerUnitId,
        String sourceSystem,
        String sourceVersion,
        int documentCount,
        int chunkCount,
        int sourceCount,
        String indexStatus,
        Instant lastSyncTime,
        Instant lastIndexTime,
        String metadataJson,
        String status,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
