package com.mw.ai.agi.knowledge.vector;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class VectorStoreProviderRegistry {
    private final Map<String, VectorStoreProvider> providers;

    public VectorStoreProviderRegistry(List<VectorStoreProvider> providers) {
        Map<String, VectorStoreProvider> values = new LinkedHashMap<>();
        providers.forEach(provider -> values.put(normalize(provider.storeType()), provider));
        this.providers = Map.copyOf(values);
    }

    public VectorStoreProvider require(String storeType) {
        String normalized = normalize(storeType);
        VectorStoreProvider provider = providers.get(normalized);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported vector store type: " + normalized);
        }
        return provider;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
