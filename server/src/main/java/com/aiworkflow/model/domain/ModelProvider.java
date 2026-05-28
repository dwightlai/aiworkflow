package com.aiworkflow.model.domain;

import java.time.Instant;

public record ModelProvider(
        String id,
        String name,
        String baseUrl,
        String apiKeyRef,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
