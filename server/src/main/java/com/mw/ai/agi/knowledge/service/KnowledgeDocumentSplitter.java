package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.chunking.ChunkProfile;
import com.mw.ai.agi.knowledge.chunking.ChunkStrategyRouter;
import com.mw.ai.agi.knowledge.chunking.CsvStructureParser;
import com.mw.ai.agi.knowledge.chunking.DefaultChunkStrategyRouter;
import com.mw.ai.agi.knowledge.chunking.DocumentStructure;
import com.mw.ai.agi.knowledge.chunking.MarkdownStructureParser;
import com.mw.ai.agi.knowledge.chunking.OfficeStructureParser;
import com.mw.ai.agi.knowledge.chunking.PlainTextStructureParser;
import com.mw.ai.agi.knowledge.chunking.StructuredDocumentParser;
import com.mw.ai.agi.knowledge.chunking.TokenCounter;
import com.mw.ai.agi.knowledge.chunking.TokenWindowSplitter;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class KnowledgeDocumentSplitter {
    private final TokenCounter tokenCounter;
    private final List<StructuredDocumentParser> parsers;
    private final ChunkStrategyRouter strategyRouter;
    private final SemanticBoundaryChunker semanticBoundaryChunker;
    private final TokenWindowSplitter tokenWindowSplitter;

    @Autowired
    public KnowledgeDocumentSplitter(
            TokenCounter tokenCounter,
            List<StructuredDocumentParser> parsers,
            ChunkStrategyRouter strategyRouter,
            SemanticBoundaryChunker semanticBoundaryChunker
    ) {
        this.tokenCounter = tokenCounter;
        this.parsers = List.copyOf(parsers);
        this.strategyRouter = strategyRouter;
        this.semanticBoundaryChunker = semanticBoundaryChunker;
        this.tokenWindowSplitter = new TokenWindowSplitter(tokenCounter);
    }

    public KnowledgeDocumentSplitter(
            TokenCounter tokenCounter,
            EmbeddingClient embeddingClient
    ) {
        this(
                tokenCounter,
                List.of(
                        new MarkdownStructureParser(),
                        new OfficeStructureParser(),
                        new PlainTextStructureParser(),
                        new CsvStructureParser()
                ),
                new DefaultChunkStrategyRouter(tokenCounter),
                new SemanticBoundaryChunker(embeddingClient, tokenCounter)
        );
    }

    public List<KnowledgeChunkPreview> splitDocument(
            String fileName,
            String content,
            KnowledgeSplitRequest request
    ) {
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            return List.of();
        }
        String splitterType = request.effectiveSplitterType();
        if (!usesDocumentStructure(splitterType)) {
            return splitText(normalized, request);
        }
        StructuredDocumentParser parser = parsers.stream()
                .filter(candidate -> candidate.supports(fileName))
                .findFirst()
                .orElseGet(PlainTextStructureParser::new);
        DocumentStructure document = parser.parse(fileName, normalized);
        ChunkProfile profile = ChunkProfile.defaults(
                request.effectiveSplitterType(),
                request.effectiveChunkSize(),
                request.effectiveChunkOverlap()
        );
        return strategyRouter.split(document, profile);
    }

    private boolean usesDocumentStructure(String splitterType) {
        return "STRUCTURE_AWARE".equals(splitterType)
                || "MARKDOWN_HEADING".equals(splitterType)
                || "STRUCTURED_TABLE".equals(splitterType)
                || "TABLE_ROW".equals(splitterType);
    }

    public List<KnowledgeChunkPreview> splitText(String content, KnowledgeSplitRequest request) {
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            return List.of();
        }
        String type = request.effectiveSplitterType();
        if ("STRUCTURE_AWARE".equals(type) || "MARKDOWN_HEADING".equals(type)) {
            return splitDocument("document.md", normalized, request);
        }
        List<String> chunks = switch (type) {
            case "PARAGRAPH" -> splitParagraph(normalized, request.effectiveChunkSize());
            case "SENTENCE_BOUNDARY" -> splitSentenceBoundary(normalized, request.effectiveChunkSize());
            case "SEMANTIC" -> semanticBoundaryChunker.split(normalized, request, request.embeddingModelId());
            case "SYMBOL" -> splitSymbol(normalized, request);
            default -> splitFixed(normalized, request.effectiveChunkSize(), request.effectiveChunkOverlap());
        };
        return previews(chunks, type);
    }

    public List<KnowledgeChunkPreview> splitTableRows(
            List<TableDocumentParser.TableRow> rows,
            KnowledgeSplitRequest request
    ) {
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
            if (tokenCounter.count(rowText, null) > size) {
                flush(chunks, current);
                chunks.add(rowText);
                continue;
            }
            if (!current.isEmpty()
                    && tokenCounter.count(current + "\n" + rowText, null) > size) {
                flush(chunks, current);
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(rowText);
        }
        flush(chunks, current);
        return previews(chunks, "TABLE_ROW");
    }

    private List<String> splitFixed(String content, int size, int overlap) {
        return tokenWindowSplitter.split(content, size, overlap, null);
    }

    private List<String> splitParagraph(String content, int size) {
        List<String> chunks = new ArrayList<>();
        for (String paragraph : content.split("\\R{2,}|\\R")) {
            String trimmed = paragraph.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (tokenCounter.count(trimmed, null) <= size) {
                chunks.add(trimmed);
            } else {
                chunks.addAll(splitFixed(trimmed, size, 0));
            }
        }
        return chunks;
    }

    private List<String> splitSentenceBoundary(String content, int size) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : content.split(
                "(?<=[。！？；：!?;:])\\s*|\\R+|(?<!\\d\\.)(?<=[.!?])\\s+"
        )) {
            String trimmed = sentence.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (tokenCounter.count(trimmed, null) > size) {
                flush(chunks, current);
                chunks.addAll(splitFixed(trimmed, size, 0));
                continue;
            }
            if (!current.isEmpty()
                    && tokenCounter.count(current + " " + trimmed, null) > size) {
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
            return splitFixed(content, request.effectiveChunkSize(), request.effectiveChunkOverlap());
        }
        List<String> chunks = new ArrayList<>();
        for (String part : content.split(Pattern.quote(separator), -1)) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (tokenCounter.count(trimmed, null) <= request.effectiveChunkSize()) {
                chunks.add(trimmed);
            } else {
                chunks.addAll(splitFixed(trimmed, request.effectiveChunkSize(), request.effectiveChunkOverlap()));
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

    private List<KnowledgeChunkPreview> previews(List<String> chunks, String type) {
        List<KnowledgeChunkPreview> previews = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            String content = chunks.get(index);
            previews.add(new KnowledgeChunkPreview(
                    index,
                    content,
                    tokenCounter.count(content, null),
                    normalizeLegacyType(type),
                    null,
                    List.of(),
                    null,
                    null,
                    "CHILD",
                    false,
                    content,
                    "{}",
                    "legacy-" + type.toLowerCase()
            ));
        }
        return previews;
    }

    private String normalizeLegacyType(String type) {
        return switch (type) {
            case "SENTENCE_BOUNDARY", "SEMANTIC" -> "PARAGRAPH";
            case "STRUCTURED_TABLE", "TABLE_ROW" -> "TABLE_ROW";
            default -> type;
        };
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
