package com.mw.ai.agi.generation.domain;

import java.time.Instant;

public record GenerationJob(
        String id,
        String tenantId,
        String botId,
        String templateId,
        String workflowId,
        String unitId,
        String userId,
        String knowledgeBaseIds,
        String externalCorpusRef,
        String variables,
        String status,
        String outlineJson,
        String sectionOutputsJson,
        String workflowRunSnapshot,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
}
