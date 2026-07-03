package com.mw.ai.agi.knowledge.domain;

import java.util.List;

public record KnowledgeChunkPreview(
        int index,
        String content,
        int tokenEstimate,
        String chunkType,
        String chunkTitle,
        List<String> sectionPath,
        String parentChunkId,
        String groupId,
        String chunkLevel,
        boolean atomic,
        String embeddingContent,
        String metadataJson,
        String splitReason
) {
    public KnowledgeChunkPreview(int index, String content, int tokenEstimate) {
        this(
                index,
                content,
                tokenEstimate,
                "PARAGRAPH",
                null,
                List.of(),
                null,
                null,
                "CHILD",
                false,
                content,
                "{}",
                "legacy"
        );
    }

    public KnowledgeChunkPreview {
        sectionPath = sectionPath == null ? List.of() : List.copyOf(sectionPath);
        chunkType = chunkType == null || chunkType.isBlank() ? "PARAGRAPH" : chunkType;
        chunkLevel = chunkLevel == null || chunkLevel.isBlank() ? "CHILD" : chunkLevel;
        embeddingContent = embeddingContent == null ? content : embeddingContent;
        metadataJson = metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson;
    }
}
