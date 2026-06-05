package com.mw.ai.agi.knowledge.domain;

public record KnowledgeChunkPreview(
        int index,
        String content,
        int tokenEstimate
) {
}
