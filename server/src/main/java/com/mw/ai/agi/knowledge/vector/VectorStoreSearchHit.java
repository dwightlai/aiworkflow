package com.mw.ai.agi.knowledge.vector;

public record VectorStoreSearchHit(
        String chunkId,
        String documentId,
        String documentName,
        String content,
        String datasetId,
        double score,
        String metadataJson
) {
}
