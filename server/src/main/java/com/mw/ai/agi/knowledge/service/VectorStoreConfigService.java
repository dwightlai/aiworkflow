package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.mw.ai.agi.knowledge.vector.ElasticsearchVectorStoreProvider;
import com.mw.ai.agi.knowledge.vector.VectorStoreConnectionResult;
import com.mw.ai.agi.knowledge.vector.VectorStoreProviderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VectorStoreConfigService {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 30000;
    private static final int DEFAULT_VECTOR_DIMENSION = 1536;
    private final VectorStoreConfigStore store;
    private final VectorStoreProviderRegistry providers;

    public VectorStoreConfigService() {
        this(
                new InMemoryVectorStoreConfigStore(),
                new VectorStoreProviderRegistry(List.of(new ElasticsearchVectorStoreProvider(
                        new ElasticsearchVectorStoreClient(new com.fasterxml.jackson.databind.ObjectMapper())
                )))
        );
    }

    @Autowired
    public VectorStoreConfigService(VectorStoreConfigStore store, VectorStoreProviderRegistry providers) {
        this.store = store;
        this.providers = providers;
    }

    public VectorStoreConfigService(
            VectorStoreConfigStore store,
            ElasticsearchVectorStoreClient elasticsearchVectorStoreClient
    ) {
        this(store, new VectorStoreProviderRegistry(List.of(
                new ElasticsearchVectorStoreProvider(elasticsearchVectorStoreClient)
        )));
    }

    public VectorStoreConfigService(VectorStoreConfigStore store) {
        this(store, new ElasticsearchVectorStoreClient(new com.fasterxml.jackson.databind.ObjectMapper()));
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
        return create(
                name, storeType, endpoint, indexName,
                null, null, null, indexName, DEFAULT_VECTOR_DIMENSION, false, "{}",
                username, password, apiKey, connectTimeoutMs, readTimeoutMs, enabled
        );
    }

    public VectorStoreConfig create(
            String name,
            String storeType,
            String endpoint,
            String indexName,
            String host,
            Integer port,
            String databaseName,
            String namespaceName,
            Integer vectorDimension,
            boolean sslEnabled,
            String optionsJson,
            String username,
            String password,
            String apiKey,
            Integer connectTimeoutMs,
            Integer readTimeoutMs,
            boolean enabled
    ) {
        Instant now = Instant.now();
        String normalizedType = normalizeStoreType(storeType, "MEMORY");
        VectorStoreConfig config = new VectorStoreConfig(
                "vector_" + UUID.randomUUID(),
                name,
                normalizedType,
                blankToNull(endpoint),
                defaultString(indexName, "aiworkflow_kb"),
                blankToNull(host),
                defaultPort(normalizedType, port),
                blankToNull(databaseName),
                defaultString(namespaceName, defaultString(indexName, "aiworkflow_kb")),
                positiveOrDefault(vectorDimension, DEFAULT_VECTOR_DIMENSION),
                sslEnabled,
                defaultString(optionsJson, "{}"),
                blankToNull(username),
                blankToNull(password),
                blankToNull(apiKey),
                positiveOrDefault(connectTimeoutMs, DEFAULT_CONNECT_TIMEOUT_MS),
                positiveOrDefault(readTimeoutMs, DEFAULT_READ_TIMEOUT_MS),
                enabled,
                now,
                now
        );
        ensureExternalIndexIfNeeded(config);
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
        return update(
                id, name, storeType, endpoint, indexName,
                null, null, null, null, null, false, null,
                username, password, apiKey, connectTimeoutMs, readTimeoutMs, enabled
        );
    }

    public VectorStoreConfig update(
            String id,
            String name,
            String storeType,
            String endpoint,
            String indexName,
            String host,
            Integer port,
            String databaseName,
            String namespaceName,
            Integer vectorDimension,
            boolean sslEnabled,
            String optionsJson,
            String username,
            String password,
            String apiKey,
            Integer connectTimeoutMs,
            Integer readTimeoutMs,
            boolean enabled
    ) {
        VectorStoreConfig current = store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vector store config not found: " + id));
        String normalizedType = normalizeStoreType(storeType, current.storeType());
        VectorStoreConfig updated = new VectorStoreConfig(
                current.id(),
                defaultString(name, current.name()),
                normalizedType,
                blankToNull(endpoint),
                defaultString(indexName, current.indexName()),
                host == null ? current.host() : blankToNull(host),
                port == null ? current.port() : defaultPort(normalizedType, port),
                databaseName == null ? current.databaseName() : blankToNull(databaseName),
                defaultString(namespaceName, current.namespaceName()),
                positiveOrDefault(vectorDimension, current.vectorDimension()),
                sslEnabled,
                defaultString(optionsJson, current.optionsJson()),
                blankToNull(username),
                preserveSecret(password, current.password()),
                preserveSecret(apiKey, current.apiKey()),
                positiveOrDefault(connectTimeoutMs, current.connectTimeoutMs()),
                positiveOrDefault(readTimeoutMs, current.readTimeoutMs()),
                enabled,
                current.createdAt(),
                Instant.now()
        );
        ensureExternalIndexIfNeeded(updated);
        return store.save(updated);
    }

    public void delete(String id) {
        store.findById(id).ifPresent(config -> {
            if (!"MEMORY".equalsIgnoreCase(config.storeType())) {
                providers.require(config.storeType()).close(config);
            }
        });
        store.delete(id);
    }

    public VectorStoreConnectionResult testConnection(VectorStoreConfig config) {
        if ("MEMORY".equalsIgnoreCase(config.storeType())) {
            return new VectorStoreConnectionResult(true, "MEMORY", null, 0, "Local memory store");
        }
        return providers.require(config.storeType()).testConnection(config);
    }

    public VectorStoreConnectionResult testConnection(String id) {
        return testConnection(store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vector store config not found: " + id)));
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

    private void ensureExternalIndexIfNeeded(VectorStoreConfig config) {
        if (!config.enabled()
                || "MEMORY".equalsIgnoreCase(config.storeType())) {
            return;
        }
        providers.require(config.storeType()).ensureStore(config, config.vectorDimension());
    }

    private Integer defaultPort(String storeType, Integer port) {
        if (port != null && port > 0) {
            return port;
        }
        return switch (storeType) {
            case "MILVUS" -> 19530;
            case "PGVECTOR" -> 5432;
            default -> null;
        };
    }
}
