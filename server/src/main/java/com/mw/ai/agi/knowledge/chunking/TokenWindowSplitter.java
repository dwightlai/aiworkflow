package com.mw.ai.agi.knowledge.chunking;

import java.util.ArrayList;
import java.util.List;

public final class TokenWindowSplitter {
    private final TokenCounter tokenCounter;

    public TokenWindowSplitter(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }

    public List<String> split(String text, int maxTokens, int overlapTokens, String modelId) {
        String value = text == null ? "" : text.trim();
        if (value.isBlank()) {
            return List.of();
        }
        int limit = Math.max(1, maxTokens);
        int overlap = Math.max(0, Math.min(overlapTokens, limit / 2));
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < value.length(); ) {
            int end = maximumEnd(value, start, limit, modelId);
            if (end <= start) {
                end = Math.min(value.length(), start + 1);
            }
            String chunk = value.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= value.length()) {
                break;
            }
            int next = overlap == 0 ? end : suffixStart(value, start, end, overlap, modelId);
            start = Math.max(start + 1, next);
        }
        return List.copyOf(chunks);
    }

    public String suffix(String text, int tokens, String modelId) {
        String value = text == null ? "" : text.trim();
        if (value.isBlank() || tokens <= 0) {
            return "";
        }
        return value.substring(suffixStart(value, 0, value.length(), tokens, modelId)).trim();
    }

    private int maximumEnd(String text, int start, int maxTokens, String modelId) {
        int low = start + 1;
        int high = text.length();
        int best = start;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            if (tokenCounter.count(text.substring(start, middle), modelId) <= maxTokens) {
                best = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return best;
    }

    private int suffixStart(String text, int lowerBound, int end, int overlapTokens, String modelId) {
        int low = lowerBound;
        int high = end;
        int best = end;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            if (tokenCounter.count(text.substring(middle, end), modelId) <= overlapTokens) {
                best = middle;
                high = middle - 1;
            } else {
                low = middle + 1;
            }
        }
        return best;
    }
}
