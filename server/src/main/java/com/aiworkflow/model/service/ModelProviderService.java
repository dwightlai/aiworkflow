package com.aiworkflow.model.service;

import com.aiworkflow.model.domain.ModelProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ModelProviderService {
    private final List<ModelProvider> providers = new CopyOnWriteArrayList<>();

    public ModelProvider create(
            String name,
            String modelType,
            String description,
            boolean visionSupport,
            BigDecimal pricePerMillionTokens,
            String baseUrl,
            String model,
            String apiKeyRef,
            boolean enabled
    ) {
        Instant now = Instant.now();
        ModelProvider provider = new ModelProvider(
                "model_provider_" + UUID.randomUUID(),
                name,
                modelType,
                description,
                visionSupport,
                pricePerMillionTokens,
                baseUrl,
                model,
                apiKeyRef,
                enabled,
                now,
                now
        );
        providers.add(provider);
        return provider;
    }

    public ModelProvider update(
            String id,
            String name,
            String modelType,
            String description,
            boolean visionSupport,
            BigDecimal pricePerMillionTokens,
            String baseUrl,
            String model,
            String apiKeyRef,
            boolean enabled
    ) {
        for (int index = 0; index < providers.size(); index += 1) {
            ModelProvider current = providers.get(index);
            if (current.id().equals(id)) {
                ModelProvider updated = new ModelProvider(
                        current.id(),
                        name,
                        modelType,
                        description,
                        visionSupport,
                        pricePerMillionTokens,
                        baseUrl,
                        model,
                        apiKeyRef,
                        enabled,
                        current.createdAt(),
                        Instant.now()
                );
                providers.set(index, updated);
                return updated;
            }
        }
        throw new IllegalArgumentException("Model provider not found: " + id);
    }

    public List<ModelProvider> list() {
        return new ArrayList<>(providers);
    }

    public ModelProvider get(String id) {
        return providers.stream()
                .filter(provider -> provider.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Model provider not found: " + id));
    }

    public void delete(String id) {
        providers.removeIf(provider -> provider.id().equals(id));
    }
}
