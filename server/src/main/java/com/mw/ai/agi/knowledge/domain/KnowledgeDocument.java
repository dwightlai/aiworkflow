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
        String errorMessage,
        String storagePath,
        String datasetId,
        String sourceIndexId,
        String topicId,
        String docType,
        String sourceSystem,
        String sourceType,
        String sourceRefId,
        String materialSourceType,
        String materialType,
        String sourceArchiveFileId,
        String sourceVersion,
        String titleSnapshot,
        String metadataJson,
        String summaryText,
        String securityLevel,
        String ownerUnitId,
        Instant lastIndexTime
) {
    public KnowledgeDocument(String id, String knowledgeBaseId, String name, int chunkCount, Instant createdAt) {
        this(
                id, knowledgeBaseId, name, chunkCount, createdAt,
                "TEXT_DOCUMENT", "READY", null, null, null, 0, "TEXT", "FIXED_LENGTH", "{}",
                null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    public KnowledgeDocument withDatasetId(String datasetId) {
        return new KnowledgeDocument(
                id, knowledgeBaseId, name, chunkCount, createdAt, datasetType, processingStatus, tags, category,
                source, rowCount, parserType, splitterType, splitterConfig, rawContent, errorMessage, storagePath,
                datasetId, sourceIndexId, topicId, docType, sourceSystem, sourceType, sourceRefId, materialSourceType,
                materialType, sourceArchiveFileId, sourceVersion, titleSnapshot, metadataJson, summaryText,
                securityLevel, ownerUnitId, lastIndexTime
        );
    }

    public KnowledgeDocument withProcessingStatus(String status, String error) {
        return new KnowledgeDocument(
                id, knowledgeBaseId, name, chunkCount, createdAt, datasetType, status, tags, category,
                source, rowCount, parserType, splitterType, splitterConfig, rawContent, error, storagePath,
                datasetId, sourceIndexId, topicId, docType, sourceSystem, sourceType, sourceRefId, materialSourceType,
                materialType, sourceArchiveFileId, sourceVersion, titleSnapshot, metadataJson, summaryText,
                securityLevel, ownerUnitId, lastIndexTime
        );
    }

    public KnowledgeDocument withChunkCount(int count) {
        return new KnowledgeDocument(
                id, knowledgeBaseId, name, count, createdAt, datasetType, processingStatus, tags, category,
                source, rowCount, parserType, splitterType, splitterConfig, rawContent, errorMessage, storagePath,
                datasetId, sourceIndexId, topicId, docType, sourceSystem, sourceType, sourceRefId, materialSourceType,
                materialType, sourceArchiveFileId, sourceVersion, titleSnapshot, metadataJson, summaryText,
                securityLevel, ownerUnitId, lastIndexTime
        );
    }
}
