package com.aiworkflow.knowledge.domain;

public record KnowledgeChunkPreview(
        int index,
        String content,
        int tokenEstimate
) {
}
