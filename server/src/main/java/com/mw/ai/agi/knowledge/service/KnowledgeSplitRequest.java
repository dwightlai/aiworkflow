package com.mw.ai.agi.knowledge.service;

public record KnowledgeSplitRequest(
        String splitterType,
        int chunkSize,
        int chunkOverlap,
        String separator,
        String embeddingModelId,
        Double semanticSimilarityThreshold
) {
    public KnowledgeSplitRequest(
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String separator,
            String embeddingModelId
    ) {
        this(splitterType, chunkSize, chunkOverlap, separator, embeddingModelId, null);
    }

    public KnowledgeSplitRequest(String splitterType, int chunkSize, int chunkOverlap, String separator) {
        this(splitterType, chunkSize, chunkOverlap, separator, null, null);
    }

    public KnowledgeSplitRequest(String splitterType, int chunkSize, String separator) {
        this(splitterType, chunkSize, 0, separator, null, null);
    }

    public int effectiveChunkSize() {
        return Math.max(1, chunkSize <= 0 ? 200 : chunkSize);
    }

    public int effectiveChunkOverlap() {
        return Math.max(0, Math.min(chunkOverlap, effectiveChunkSize() / 2));
    }

    public String effectiveSplitterType() {
        return splitterType == null || splitterType.isBlank() ? "FIXED_LENGTH" : splitterType.trim().toUpperCase();
    }

    public double effectiveSemanticSimilarityThreshold() {
        if (semanticSimilarityThreshold == null) {
            return 0.78;
        }
        return Math.max(0, Math.min(1, semanticSimilarityThreshold));
    }

    public KnowledgeSplitRequest withEmbeddingModel(String modelId) {
        return new KnowledgeSplitRequest(
                splitterType,
                chunkSize,
                chunkOverlap,
                separator,
                modelId,
                semanticSimilarityThreshold
        );
    }
}
