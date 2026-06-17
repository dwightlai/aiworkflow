package com.mw.ai.agi.knowledge.domain;

public record KnowledgeChunk(
        String id,
        String knowledgeBaseId,
        String documentId,
        String documentName,
        String content,
        int index,
        boolean enabled,
        int tokenEstimate,
        String datasetId,
        String sourceIndexId,
        String topicId,
        String chunkTitle,
        String chunkType,
        String sourceSystem,
        String sourceType,
        String sourceRefId,
        String materialSourceType,
        String materialType,
        String sourceArchiveFileId,
        String sourcePage,
        String sourcePosition,
        String citationText,
        String metadataJson,
        String securityLevel
) {
    public KnowledgeChunk(
            String id,
            String knowledgeBaseId,
            String documentId,
            String documentName,
            String content,
            int index,
            boolean enabled,
            int tokenEstimate
    ) {
        this(
                id, knowledgeBaseId, documentId, documentName, content, index, enabled, tokenEstimate,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    public KnowledgeChunk withDatasetContext(
            String datasetId,
            String sourceIndexId,
            String topicId,
            String sourceRefId,
            String materialSourceType,
            String materialType,
            String sourceArchiveFileId
    ) {
        return new KnowledgeChunk(
                id, knowledgeBaseId, documentId, documentName, content, index, enabled, tokenEstimate,
                datasetId, sourceIndexId, topicId, chunkTitle, chunkType, sourceSystem, sourceType, sourceRefId,
                materialSourceType, materialType, sourceArchiveFileId, sourcePage, sourcePosition, citationText,
                metadataJson, securityLevel
        );
    }
}
