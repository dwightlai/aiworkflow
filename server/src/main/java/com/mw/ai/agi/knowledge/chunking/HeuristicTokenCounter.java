package com.mw.ai.agi.knowledge.chunking;

import org.springframework.stereotype.Component;

@Component
public class HeuristicTokenCounter implements TokenCounter {
    @Override
    public int count(String text, String modelId) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int tokens = 0;
        boolean inWord = false;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (isCjk(value)) {
                tokens++;
                inWord = false;
            } else if (Character.isLetterOrDigit(value)) {
                if (!inWord) {
                    tokens++;
                    inWord = true;
                }
            } else if (!Character.isWhitespace(value)) {
                tokens++;
                inWord = false;
            } else {
                inWord = false;
            }
        }
        return Math.max(1, tokens);
    }

    private boolean isCjk(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }
}
