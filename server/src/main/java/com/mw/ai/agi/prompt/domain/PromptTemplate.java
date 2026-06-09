package com.mw.ai.agi.prompt.domain;

import java.time.Instant;

public record PromptTemplate(
        String id,
        String tenantId,
        String name,
        String template,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
