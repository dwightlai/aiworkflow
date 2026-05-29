package com.aiworkflow.model.domain;

import java.time.Instant;
import java.math.BigDecimal;

public record ModelProvider(
        String id,
        String name,
        String modelType,
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
