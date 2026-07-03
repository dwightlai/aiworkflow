package com.mw.ai.agi.knowledge.vector;

public record VectorStoreConnectionResult(
        boolean success,
        String storeType,
        String serverVersion,
        long latencyMs,
        String message
) {
}
