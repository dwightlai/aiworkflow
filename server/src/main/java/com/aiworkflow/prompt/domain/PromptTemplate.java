package com.aiworkflow.prompt.domain;

import java.time.Instant;

public record PromptTemplate(
        String id,
        String name,
        String template,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
