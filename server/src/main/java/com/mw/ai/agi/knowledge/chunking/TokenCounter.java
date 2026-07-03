package com.mw.ai.agi.knowledge.chunking;

public interface TokenCounter {
    int count(String text, String modelId);
}
