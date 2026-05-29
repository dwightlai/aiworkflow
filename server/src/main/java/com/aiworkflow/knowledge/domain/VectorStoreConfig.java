package com.aiworkflow.knowledge.domain;

import java.time.Instant;

public record VectorStoreConfig(
        String id,
        String name,
        String storeType,
        String endpoint,
        String indexName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
