package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;

public record KnowledgeSourceIndex(
        String id,
        String tenantId,
        String knowledgeBaseId,
        String datasetId,
        String topicId,
        String sourceSystem,
        String sourceType,
        String sourceRefId,
        String materialSourceType,
        String materialType,
        String sourceArchiveFileId,
        String sourceTitleSnapshot,
        String sourceVersion,
        String sourceUrl,
        String storagePath,
        String metadataSnapshot,
        String documentId,
        String indexStatus,
        Instant lastSyncTime,
        Instant lastIndexTime,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
