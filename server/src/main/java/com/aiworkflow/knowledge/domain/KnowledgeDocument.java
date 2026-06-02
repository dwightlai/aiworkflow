package com.aiworkflow.knowledge.domain;

import java.time.Instant;

public record KnowledgeDocument(
        String id,
        String knowledgeBaseId,
        String name,
        int chunkCount,
        Instant createdAt
) {
}
