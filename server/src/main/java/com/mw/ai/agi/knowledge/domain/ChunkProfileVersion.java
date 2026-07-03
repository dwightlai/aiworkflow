package com.mw.ai.agi.knowledge.domain;

import java.time.Instant;

public record ChunkProfileVersion(
        String id,
        String knowledgeBaseId,
        int version,
        String name,
        String strategy,
        int chunkSize,
        int chunkOverlap,
        String configJson,
        String status,
        Instant createdAt
) {
}
