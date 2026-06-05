package com.mw.ai.agi.model.service;

import com.mw.ai.agi.model.domain.ModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryModelProviderStore implements ModelProviderStore {
    private final List<ModelProvider> providers = new CopyOnWriteArrayList<>();

    @Override
    public ModelProvider save(ModelProvider provider) {
        providers.removeIf(current -> current.id().equals(provider.id()));
        providers.add(provider);
        return provider;
    }

    @Override
    public Optional<ModelProvider> findById(String id) {
        return providers.stream()
                .filter(provider -> provider.id().equals(id))
                .findFirst();
    }

    @Override
    public List<ModelProvider> list() {
        return new ArrayList<>(providers);
    }

    @Override
    public void delete(String id) {
        providers.removeIf(provider -> provider.id().equals(id));
    }
}
