package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.VectorStoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VectorStoreConfigService {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 30000;
    private final VectorStoreConfigStore store;

    public VectorStoreConfigService() {
        this(new InMemoryVectorStoreConfigStore());
    }

    @Autowired
    public VectorStoreConfigService(VectorStoreConfigStore store) {
        this.store = store;
    }

    public VectorStoreConfig create(String name, String storeType, String endpoint, String indexName, boolean enabled) {
        return create(name, storeType, endpoint, indexName, null, null, null, null, null, enabled);
    }

    public VectorStoreConfig create(
            String name,
            String storeType,
            String endpoint,
            String indexName,
            String username,
            String password,
            String apiKey,
            Integer connectTimeoutMs,
            Integer readTimeoutMs,
            boolean enabled
    ) {
        Instant now = Instant.now();
        VectorStoreConfig config = new VectorStoreConfig(
                "vector_" + UUID.randomUUID(),
                name,
                normalizeStoreType(storeType, "MEMORY"),
                blankToNull(endpoint),
                defaultString(indexName, "aiworkflow_kb"),
                blankToNull(username),
                blankToNull(password),
                blankToNull(apiKey),
                positiveOrDefault(connectTimeoutMs, DEFAULT_CONNECT_TIMEOUT_MS),
                positiveOrDefault(readTimeoutMs, DEFAULT_READ_TIMEOUT_MS),
                enabled,
                now,
                now
        );
        return store.save(config);
    }

    public List<VectorStoreConfig> list() {
        return store.list();
    }

    public VectorStoreConfig update(
            String id,
            String name,
            String storeType,
            String endpoint,
            String indexName,
            String username,
            String password,
            String apiKey,
            Integer connectTimeoutMs,
            Integer readTimeoutMs,
            boolean enabled
    ) {
        VectorStoreConfig current = store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vector store config not found: " + id));
        return store.save(new VectorStoreConfig(
                current.id(),
                defaultString(name, current.name()),
                normalizeStoreType(storeType, current.storeType()),
                blankToNull(endpoint),
                defaultString(indexName, current.indexName()),
                blankToNull(username),
                preserveSecret(password, current.password()),
                preserveSecret(apiKey, current.apiKey()),
                positiveOrDefault(connectTimeoutMs, current.connectTimeoutMs()),
                positiveOrDefault(readTimeoutMs, current.readTimeoutMs()),
                enabled,
                current.createdAt(),
                Instant.now()
        ));
    }

    public void delete(String id) {
        store.delete(id);
    }

    private String normalizeStoreType(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String preserveSecret(String next, String current) {
        return next == null || next.isBlank() ? current : next.trim();
    }

    private int positiveOrDefault(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }
}
