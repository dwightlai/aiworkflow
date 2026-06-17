package com.mw.ai.agi.generation.domain;

import java.time.Instant;

public record GenerationOutput(
        String id,
        String tenantId,
        String jobId,
        String title,
        String outputType,
        String contentMarkdown,
        String contentJson,
        String contentDocxPath,
        String outputTemplateId,
        String citations,
        String sourceSnapshot,
        String status,
        Instant createdAt
) {
}
