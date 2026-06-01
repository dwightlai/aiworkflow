package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.VectorStoreConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryVectorStoreConfigStore implements VectorStoreConfigStore {
    private final List<VectorStoreConfig> configs = new CopyOnWriteArrayList<>();

    @Override
    public VectorStoreConfig save(VectorStoreConfig config) {
        configs.removeIf(current -> current.id().equals(config.id()));
        configs.add(config);
        return config;
    }

    @Override
    public Optional<VectorStoreConfig> findById(String id) {
        return configs.stream()
                .filter(config -> config.id().equals(id))
                .findFirst();
    }

    @Override
    public List<VectorStoreConfig> list() {
        return new ArrayList<>(configs);
    }

    @Override
    public void delete(String id) {
        configs.removeIf(config -> config.id().equals(id));
    }
}
