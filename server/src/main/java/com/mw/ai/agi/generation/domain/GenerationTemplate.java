package com.mw.ai.agi.generation.domain;

import java.time.Instant;

public record GenerationTemplate(
        String id,
        String tenantId,
        String name,
        String code,
        String description,
        String category,
        String ownerUnitId,
        String outputType,
        String templateCategory,
        String docxConfig,
        String layoutConfig,
        String linkedHtmlTemplateId,
        String templateSchema,
        String workflowId,
        String workflowSnapshot,
        String status,
        int version,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
