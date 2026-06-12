package com.mw.ai.agi.connector.domain;

import java.time.Instant;

public record Connector(
        String id,
        String tenantId,
        String name,
        String code,
        String type,
        String accessType,
        String baseUrl,
        String authMode,
        String authConfig,
        boolean enabled,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
