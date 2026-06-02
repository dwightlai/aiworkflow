package com.aiworkflow.knowledge.domain;

public record KnowledgeChunk(
        String id,
        String knowledgeBaseId,
        String documentId,
        String documentName,
        String content,
        int index,
        boolean enabled,
        int tokenEstimate
) {
}
