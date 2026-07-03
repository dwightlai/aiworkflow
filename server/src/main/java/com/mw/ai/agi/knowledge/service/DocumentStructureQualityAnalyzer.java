package com.mw.ai.agi.knowledge.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentStructureQualityAnalyzer {
    private static final Pattern INLINE_MARKDOWN_HEADING =
            Pattern.compile("(?<!#)#{1,6}\\s+");
    private static final int LONG_LINE_THRESHOLD = 2000;
    private static final int INLINE_HEADING_THRESHOLD = 3;

    public List<String> analyze(String fileName, String content) {
        String extension = extension(fileName);
        if (!"md".equals(extension) && !"markdown".equals(extension)) {
            return List.of();
        }
        String value = content == null ? "" : content;
        List<String> warnings = new ArrayList<>();
        if (value.lines().anyMatch(this::hasManyInlineHeadings)) {
            warnings.add(
                    "\u68c0\u6d4b\u5230\u8d85\u957f\u5355\u884c\u4e14\u5305\u542b\u591a\u4e2a Markdown "
                            + "\u6807\u9898\u6807\u8bb0\uff0c\u65e0\u6cd5\u4fdd\u8bc1\u7ae0\u8282\u8fb9\u754c\u3002"
                            + "\u8bf7\u5148\u901a\u8fc7\u524d\u7f6e\u89e3\u6790\u6e05\u6d17\u6d41\u7a0b"
                            + "\u6062\u590d\u6807\u9898\u6362\u884c\u540e\u518d\u4e0a\u4f20\u3002"
            );
        }
        return List.copyOf(warnings);
    }

    private boolean hasManyInlineHeadings(String line) {
        if (line == null || line.length() < LONG_LINE_THRESHOLD) {
            return false;
        }
        Matcher matcher = INLINE_MARKDOWN_HEADING.matcher(line);
        int count = 0;
        while (matcher.find()) {
            if (++count >= INLINE_HEADING_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private String extension(String fileName) {
        String value = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        int dot = value.lastIndexOf('.');
        return dot < 0 ? "" : value.substring(dot + 1);
    }
}
