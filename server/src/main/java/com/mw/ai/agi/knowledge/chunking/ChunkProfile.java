package com.mw.ai.agi.knowledge.chunking;

import java.util.Map;

public record ChunkProfile(
        String strategy,
        int targetTokens,
        int maxTokens,
        int minTokens,
        int overlapTokens,
        boolean preserveAtomicBlocks,
        boolean includeSectionPath,
        Map<String, Object> options
) {
    public ChunkProfile {
        strategy = strategy == null || strategy.isBlank() ? "STRUCTURE_AWARE" : strategy.trim().toUpperCase();
        targetTokens = targetTokens <= 0 ? 500 : targetTokens;
        maxTokens = maxTokens <= 0 ? Math.max(targetTokens, 800) : Math.max(maxTokens, targetTokens);
        minTokens = Math.max(0, Math.min(minTokens, targetTokens));
        overlapTokens = Math.max(0, Math.min(overlapTokens, targetTokens / 2));
        options = options == null ? Map.of() : Map.copyOf(options);
    }

    public static ChunkProfile defaults(String strategy, int chunkSize, int overlap) {
        int max = chunkSize <= 0 ? 800 : chunkSize;
        return new ChunkProfile(
                strategy,
                max,
                max,
                Math.max(1, max / 4),
                overlap,
                true,
                true,
                Map.of()
        );
    }
}
