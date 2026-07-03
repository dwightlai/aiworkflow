package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;

public record VectorStoreConfig(
        String id,
        String name,
        String storeType,
        String endpoint,
        String indexName,
        String host,
        Integer port,
        String databaseName,
        String namespaceName,
        int vectorDimension,
        boolean sslEnabled,
        String optionsJson,
        String username,
        String password,
        String apiKey,
        int connectTimeoutMs,
        int readTimeoutMs,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public VectorStoreConfig(
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
        this(
                id, name, storeType, endpoint, indexName,
                null, null, null, indexName, 1536, false, "{}",
                username, password, apiKey, connectTimeoutMs, readTimeoutMs,
                enabled, createdAt, updatedAt
        );
    }
}
