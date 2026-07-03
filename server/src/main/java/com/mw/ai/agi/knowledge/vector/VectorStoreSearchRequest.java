package com.mw.ai.agi.knowledge.vector;

import java.util.List;

public record VectorStoreSearchRequest(
        String knowledgeBaseId,
        String datasetId,
        List<Double> queryEmbedding,
        int topK,
        double similarityThreshold
) {
}
