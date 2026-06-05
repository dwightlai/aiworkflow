package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;
import java.util.List;

public record KnowledgeChunkVector(
        String chunkId,
        String knowledgeBaseId,
        String documentId,
        String embeddingModelId,
        List<Double> embedding,
        Instant createdAt
) {
}
