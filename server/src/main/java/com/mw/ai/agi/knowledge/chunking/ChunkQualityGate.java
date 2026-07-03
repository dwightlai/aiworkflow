package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;

import java.util.List;

public final class ChunkQualityGate {

    public void validate(List<KnowledgeChunkPreview> chunks, int maxTokens) {
        int limit = Math.max(1, maxTokens);
        for (KnowledgeChunkPreview chunk : chunks == null ? List.<KnowledgeChunkPreview>of() : chunks) {
            if (!chunk.atomic() && chunk.tokenEstimate() > limit) {
                throw new IllegalStateException(
                        "Non-atomic chunk exceeds token limit: "
                                + chunk.tokenEstimate()
                                + " > "
                                + limit
                                + " at chunk #"
                                + chunk.index()
                );
            }
        }
    }
}
