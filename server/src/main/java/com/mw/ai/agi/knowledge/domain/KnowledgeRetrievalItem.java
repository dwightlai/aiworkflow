package com.mw.ai.agi.knowledge.domain;

import java.util.Map;

public record KnowledgeRetrievalItem(
        String chunkId,
        String documentId,
        String sourceIndexId,
        String content,
        int score,
        String sourceTitle,
        String sourceRefId,
        String sourceArchiveFileId,
        String sourcePage,
        String citationText,
        Map<String, Object> metadata
) {
}
