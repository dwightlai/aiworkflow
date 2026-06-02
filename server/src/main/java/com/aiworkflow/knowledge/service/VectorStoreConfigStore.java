package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.VectorStoreConfig;

import java.util.List;
import java.util.Optional;

public interface VectorStoreConfigStore {
    VectorStoreConfig save(VectorStoreConfig config);

    Optional<VectorStoreConfig> findById(String id);

    List<VectorStoreConfig> list();

    void delete(String id);
}
