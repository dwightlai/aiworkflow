package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class KnowledgeDocumentSplitter {
    private final KnowledgeSplitter tokenEstimator;

    public KnowledgeDocumentSplitter(KnowledgeSplitter tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    public List<KnowledgeChunkPreview> splitText(String content, KnowledgeSplitRequest request) {
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> chunks = switch (request.effectiveSplitterType()) {
            case "PARAGRAPH" -> splitParagraph(normalized, request.effectiveChunkSize());
            case "SEMANTIC" -> splitSemantic(normalized, request.effectiveChunkSize());
            case "SYMBOL" -> splitSymbol(normalized, request);
            default -> splitFixed(normalized, request.effectiveChunkSize());
        };
        return previews(chunks);
    }

    public List<KnowledgeChunkPreview> splitTableRows(List<TableDocumentParser.TableRow> rows, KnowledgeSplitRequest request) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        int size = request.effectiveChunkSize();
        List<String> chunks = new ArrayList<>();
        String currentSheet = null;
        StringBuilder current = new StringBuilder();
        for (TableDocumentParser.TableRow row : rows) {
            String rowText = rowText(row);
            if (rowText.isBlank()) {
                continue;
            }
            if (currentSheet != null && !currentSheet.equals(row.sheetName())) {
                flush(chunks, current);
            }
            currentSheet = row.sheetName();
            if (rowText.length() > size) {
                flush(chunks, current);
                chunks.addAll(splitFixed(rowText, size));
                continue;
            }
            if (!current.isEmpty() && current.length() + 1 + rowText.length() > size) {
                flush(chunks, current);
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(rowText);
        }
        flush(chunks, current);
        return previews(chunks);
    }

    private List<String> splitFixed(String content, int size) {
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < content.length(); start += size) {
            chunks.add(content.substring(start, Math.min(start + size, content.length())).trim());
        }
        return chunks.stream().filter(chunk -> !chunk.isBlank()).toList();
    }

    private List<String> splitParagraph(String content, int size) {
        List<String> chunks = new ArrayList<>();
        for (String paragraph : content.split("\\R{2,}|\\R")) {
            String trimmed = paragraph.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.length() <= size) {
                chunks.add(trimmed);
            } else {
                chunks.addAll(splitFixed(trimmed, size));
            }
        }
        return chunks;
    }

    private List<String> splitSemantic(String content, int size) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : content.split("(?<=[銆傦紒锛?!?])\\s*")) {
            String trimmed = sentence.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.length() > size) {
                flush(chunks, current);
                chunks.addAll(splitFixed(trimmed, size));
                continue;
            }
            if (!current.isEmpty() && current.length() + 1 + trimmed.length() > size) {
                flush(chunks, current);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(trimmed);
        }
        flush(chunks, current);
        return chunks.isEmpty() ? splitParagraph(content, size) : chunks;
    }

    private List<String> splitSymbol(String content, KnowledgeSplitRequest request) {
        String separator = request.separator();
        if (separator == null || separator.isBlank()) {
            return splitFixed(content, request.effectiveChunkSize());
        }
        List<String> chunks = new ArrayList<>();
        for (String part : content.split(Pattern.quote(separator), -1)) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.length() <= request.effectiveChunkSize()) {
                chunks.add(trimmed);
            } else {
                chunks.addAll(splitFixed(trimmed, request.effectiveChunkSize()));
            }
        }
        return chunks;
    }

    private String rowText(TableDocumentParser.TableRow row) {
        StringBuilder builder = new StringBuilder("Sheet: ").append(row.sheetName());
        for (Map.Entry<String, String> cell : row.cells().entrySet()) {
            if (cell.getValue() != null && !cell.getValue().isBlank()) {
                builder.append('\n').append(cell.getKey()).append(": ").append(cell.getValue());
            }
        }
        return builder.toString();
    }

    private List<KnowledgeChunkPreview> previews(List<String> chunks) {
        List<KnowledgeChunkPreview> previews = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            String content = chunks.get(index);
            previews.add(new KnowledgeChunkPreview(index, content, tokenEstimator.estimateTokens(content)));
        }
        return previews;
    }

    private void flush(List<String> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
            current.setLength(0);
        }
    }

    private String normalize(String content) {
        return content == null ? "" : content
                .replace("\uFEFF", "")
                .replace("\u0000", "")
                .trim();
    }
}
