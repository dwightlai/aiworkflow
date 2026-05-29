package com.aiworkflow.knowledge.domain;

import java.time.Instant;

public record KnowledgeBase(
        String id,
        String name,
        String description,
        int documentCount,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt
) {
}
