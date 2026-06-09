package com.mw.ai.agi.model.domain;

import java.time.Instant;
import java.math.BigDecimal;

public record ModelProvider(
        String id,
        String tenantId,
        String ownerUnitId,
        String name,
        String modelType,
        String modelUsage,
        String description,
        boolean visionSupport,
        BigDecimal pricePerMillionTokens,
        String baseUrl,
        String model,
        String apiKeyRef,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
