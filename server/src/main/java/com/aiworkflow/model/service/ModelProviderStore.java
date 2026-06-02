package com.aiworkflow.model.service;

import com.aiworkflow.model.domain.ModelProvider;

import java.util.List;
import java.util.Optional;

public interface ModelProviderStore {
    ModelProvider save(ModelProvider provider);

    Optional<ModelProvider> findById(String id);

    List<ModelProvider> list();

    void delete(String id);
}
