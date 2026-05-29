package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeChunkPreview;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeSplitter {
    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int MAX_CHUNK_SIZE = 2000;

    public List<KnowledgeChunkPreview> preview(String content, String splitterType, int chunkSize, int chunkOverlap) {
        List<String> chunks = split(content, splitterType, chunkSize, chunkOverlap);
        List<KnowledgeChunkPreview> previews = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index += 1) {
            previews.add(new KnowledgeChunkPreview(index, chunks.get(index), estimateTokens(chunks.get(index))));
        }
        return previews;
    }

    public List<String> split(String content, String splitterType, int chunkSize, int chunkOverlap) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        int size = normalizeChunkSize(chunkSize);
        int overlap = Math.max(0, Math.min(chunkOverlap, size / 2));
        List<String> units = "MARKDOWN_HEADING".equalsIgnoreCase(nullToEmpty(splitterType))
                ? splitMarkdownSections(normalized)
                : splitParagraphs(normalized);

        List<String> result = new ArrayList<>();
        for (String unit : units) {
            addSizedChunks(result, unit, size, overlap);
        }
        return result.isEmpty() ? List.of(normalized) : result;
    }

    public int estimateTokens(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(normalized.length() / 4.0));
    }

    private List<String> splitMarkdownSections(String content) {
        String[] lines = content.split("\\R");
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("#") && !current.isEmpty()) {
                sections.add(current.toString().trim());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append("\n");
            }
            current.append(line);
        }
        if (!current.isEmpty()) {
            sections.add(current.toString().trim());
        }
        return sections;
    }

    private List<String> splitParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();
        for (String paragraph : content.split("\\R{2,}")) {
            String trimmed = paragraph.trim();
            if (!trimmed.isBlank()) {
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }

    private void addSizedChunks(List<String> result, String content, int size, int overlap) {
        for (int start = 0; start < content.length(); ) {
            int end = Math.min(start + size, content.length());
            result.add(content.substring(start, end).trim());
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
    }

    private int normalizeChunkSize(int chunkSize) {
        if (chunkSize <= 0) {
            return DEFAULT_CHUNK_SIZE;
        }
        return Math.min(chunkSize, MAX_CHUNK_SIZE);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
