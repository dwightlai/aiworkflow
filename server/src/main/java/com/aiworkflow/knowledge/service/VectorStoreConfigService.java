package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.VectorStoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VectorStoreConfigService {
    private final VectorStoreConfigStore store;

    public VectorStoreConfigService() {
        this(new InMemoryVectorStoreConfigStore());
    }

    @Autowired
    public VectorStoreConfigService(VectorStoreConfigStore store) {
        this.store = store;
    }

    public VectorStoreConfig create(String name, String storeType, String endpoint, String indexName, boolean enabled) {
        Instant now = Instant.now();
        VectorStoreConfig config = new VectorStoreConfig(
                "vector_" + UUID.randomUUID(),
                name,
                storeType == null || storeType.isBlank() ? "MEMORY" : storeType,
                endpoint,
                indexName,
                enabled,
                now,
                now
        );
        return store.save(config);
    }

    public List<VectorStoreConfig> list() {
        return store.list();
    }

    public VectorStoreConfig update(String id, String name, String storeType, String endpoint, String indexName, boolean enabled) {
        VectorStoreConfig current = store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vector store config not found: " + id));
        return store.save(new VectorStoreConfig(
                current.id(),
                name,
                storeType == null || storeType.isBlank() ? current.storeType() : storeType,
                endpoint,
                indexName,
                enabled,
                current.createdAt(),
                Instant.now()
        ));
    }

    public void delete(String id) {
        store.delete(id);
    }
}
