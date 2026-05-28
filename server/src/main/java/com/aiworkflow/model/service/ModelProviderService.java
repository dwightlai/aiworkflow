package com.aiworkflow.model.service;

import com.aiworkflow.model.domain.ModelProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ModelProviderService {
    private final List<ModelProvider> providers = new CopyOnWriteArrayList<>();

    public ModelProvider create(String name, String baseUrl, String apiKeyRef, boolean enabled) {
        Instant now = Instant.now();
        ModelProvider provider = new ModelProvider(
                "model_provider_" + UUID.randomUUID(),
                name,
                baseUrl,
                apiKeyRef,
                enabled,
                now,
                now
        );
        providers.add(provider);
        return provider;
    }

    public List<ModelProvider> list() {
        return new ArrayList<>(providers);
    }
}
