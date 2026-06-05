package com.mw.ai.agi.knowledge.service;

public record KnowledgeSplitRequest(
        String splitterType,
        int chunkSize,
        String separator
) {
    public int effectiveChunkSize() {
        return Math.max(1, chunkSize <= 0 ? 200 : chunkSize);
    }

    public String effectiveSplitterType() {
        return splitterType == null || splitterType.isBlank() ? "FIXED_LENGTH" : splitterType.trim().toUpperCase();
    }
}
