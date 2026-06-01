package com.aiworkflow.model.service;

import com.aiworkflow.model.domain.ModelProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ModelProviderService {
    private final ModelProviderStore store;

    public ModelProviderService() {
        this(new InMemoryModelProviderStore());
    }

    @Autowired
    public ModelProviderService(ModelProviderStore store) {
        this.store = store;
    }

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
        return store.save(provider);
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
        ModelProvider current = get(id);
        return store.save(new ModelProvider(
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
        ));
    }

    public List<ModelProvider> list() {
        return store.list();
    }

    public ModelProvider get(String id) {
        return store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model provider not found: " + id));
    }

    public void delete(String id) {
        store.delete(id);
    }
}
