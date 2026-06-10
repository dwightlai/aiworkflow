package com.mw.ai.agi.model.service;

import com.mw.ai.agi.model.domain.ModelProvider;

public final class ModelProviderStubSupport {
    private ModelProviderStubSupport() {
    }

    public static boolean isStubPlaceholder(ModelProvider provider) {
        if (provider == null) {
            return true;
        }
        if (!"Stub".equalsIgnoreCase(provider.modelType())) {
            String apiKeyRef = normalize(provider.apiKeyRef());
            return "stub".equalsIgnoreCase(apiKeyRef);
        }
        String apiKeyRef = normalize(provider.apiKeyRef());
        return apiKeyRef.isBlank() || "stub".equalsIgnoreCase(apiKeyRef);
    }

    public static boolean isChatUsage(ModelProvider provider) {
        String usage = provider.modelUsage() == null ? "" : provider.modelUsage().trim();
        return "CHAT".equalsIgnoreCase(usage) || "MULTIMODAL".equalsIgnoreCase(usage);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
