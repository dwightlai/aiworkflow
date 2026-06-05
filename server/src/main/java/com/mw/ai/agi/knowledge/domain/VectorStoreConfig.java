package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;

public record VectorStoreConfig(
        String id,
        String name,
        String storeType,
        String endpoint,
        String indexName,
        String username,
        String password,
        String apiKey,
        int connectTimeoutMs,
        int readTimeoutMs,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
