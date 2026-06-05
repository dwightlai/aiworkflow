package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;

import java.util.List;
import java.util.Optional;

public interface VectorStoreConfigStore {
    VectorStoreConfig save(VectorStoreConfig config);

    Optional<VectorStoreConfig> findById(String id);

    List<VectorStoreConfig> list();

    void delete(String id);
}
