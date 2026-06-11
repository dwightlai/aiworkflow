package com.mw.ai.agi.model.service;

import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.model.domain.ModelProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ModelProviderService {
    private final ModelProviderStore store;
    private final TenantBusinessGuard tenantGuard;

    public ModelProviderService() {
        this(new InMemoryModelProviderStore(), null);
    }

    public ModelProviderService(ModelProviderStore store) {
        this(store, null);
    }

    @Autowired
    public ModelProviderService(ModelProviderStore store, TenantBusinessGuard tenantGuard) {
        this.store = store;
        this.tenantGuard = tenantGuard;
    }

    public ModelProvider create(
            String name,
            String modelType,
            String modelUsage,
            String description,
            boolean visionSupport,
            BigDecimal pricePerMillionTokens,
            String baseUrl,
            String model,
            String apiKeyRef,
            boolean enabled
    ) {
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        ModelProvider provider = new ModelProvider(
                "model_provider_" + UUID.randomUUID(),
                currentTenantId(),
                null,
                name,
                modelType,
                normalizeUsage(modelUsage),
                description,
                visionSupport,
                pricePerMillionTokens,
                baseUrl,
                model,
                apiKeyRef,
                enabled,
                operator,
                operator,
                now,
                now
        );
        return store.save(provider);
    }

    public ModelProvider update(
            String id,
            String name,
            String modelType,
            String modelUsage,
            String description,
            boolean visionSupport,
            BigDecimal pricePerMillionTokens,
            String baseUrl,
            String model,
            String apiKeyRef,
            boolean enabled
    ) {
        ModelProvider current = get(id);
        String operator = OperatorContext.currentUserId();
        return store.save(new ModelProvider(
                current.id(),
                current.tenantId(),
                current.ownerUnitId(),
                name,
                modelType,
                normalizeUsage(modelUsage),
                description,
                visionSupport,
                pricePerMillionTokens,
                baseUrl,
                model,
                apiKeyRef,
                enabled,
                current.createdBy(),
                operator,
                current.createdAt(),
                Instant.now()
        ));
    }

    public List<ModelProvider> list() {
        return store.list(listTenantId());
    }

    public ModelProvider get(String id) {
        ModelProvider provider = store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model provider not found: " + id));
        assertTenantAccessible(provider.tenantId());
        return provider;
    }

    public void delete(String id) {
        get(id);
        store.delete(id);
    }

    private String currentTenantId() {
        return tenantGuard == null ? TenantContext.requireTenantId() : tenantGuard.currentTenantId();
    }

    private String listTenantId() {
        return currentTenantId();
    }

    private void assertTenantAccessible(String resourceTenantId) {
        if (tenantGuard != null) {
            tenantGuard.assertAccessible(resourceTenantId);
        }
    }

    private String normalizeUsage(String modelUsage) {
        return modelUsage == null || modelUsage.isBlank() ? "CHAT" : modelUsage.trim().toUpperCase();
    }
}
