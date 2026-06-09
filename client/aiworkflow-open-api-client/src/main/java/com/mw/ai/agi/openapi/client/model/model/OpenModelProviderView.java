package com.mw.ai.agi.openapi.client.model.model;

public record OpenModelProviderView(
        String id,
        String name,
        String modelType,
        String modelUsage,
        String description,
        boolean visionSupport,
        String model,
        boolean enabled
) {
}
