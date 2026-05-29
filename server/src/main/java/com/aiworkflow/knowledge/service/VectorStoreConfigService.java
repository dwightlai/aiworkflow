package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.VectorStoreConfig;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class VectorStoreConfigService {
    private final List<VectorStoreConfig> configs = new CopyOnWriteArrayList<>();

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
        configs.add(config);
        return config;
    }

    public List<VectorStoreConfig> list() {
        return new ArrayList<>(configs);
    }

    public VectorStoreConfig update(String id, String name, String storeType, String endpoint, String indexName, boolean enabled) {
        for (int index = 0; index < configs.size(); index += 1) {
            VectorStoreConfig current = configs.get(index);
            if (current.id().equals(id)) {
                VectorStoreConfig updated = new VectorStoreConfig(
                        current.id(),
                        name,
                        storeType == null || storeType.isBlank() ? current.storeType() : storeType,
                        endpoint,
                        indexName,
                        enabled,
                        current.createdAt(),
                        Instant.now()
                );
                configs.set(index, updated);
                return updated;
            }
        }
        throw new IllegalArgumentException("Vector store config not found: " + id);
    }
}
