package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeDocumentSplitterTest {
    private final KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(new KnowledgeSplitter());

    @Test
    void splitsTextByFixedLength() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText("abcdef", new KnowledgeSplitRequest("FIXED_LENGTH", 2, null));

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("ab", "cd", "ef");
    }

    @Test
    void splitsParagraphsAndOnlyCutsParagraphsThatExceedLength() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "short\n\nparagraph two is long",
                new KnowledgeSplitRequest("PARAGRAPH", 10, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("short", "paragraph", "two is lon", "g");
    }

    @Test
    void splitsTextByLocalSemanticSentenceBoundaries() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "A sentence. Another sentence. Last.",
                new KnowledgeSplitRequest("SEMANTIC", 25, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("A sentence.", "Another sentence. Last.");
    }

    @Test
    void splitsTextByCustomSymbolThenCutsOverflow() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "a|bcdef|gh",
                new KnowledgeSplitRequest("SYMBOL", 3, "|")
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("a", "bcd", "ef", "gh");
    }

    @Test
    void splitsTableRowsBySheetAndAggregatesRowsWithinLength() {
        List<TableDocumentParser.TableRow> rows = List.of(
                new TableDocumentParser.TableRow("Sheet1", Map.of("Field A", "A1", "Field B", "B1")),
                new TableDocumentParser.TableRow("Sheet1", Map.of("Field A", "A2", "Field B", "B2")),
                new TableDocumentParser.TableRow("Sheet2", Map.of("Field A", "A3"))
        );

        List<KnowledgeChunkPreview> chunks = splitter.splitTableRows(
                rows,
                new KnowledgeSplitRequest("STRUCTURED_TABLE", 200, null)
        );

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).content()).contains("Sheet: Sheet1", "Field A: A1", "Field A: A2");
        assertThat(chunks.get(1).content()).contains("Sheet: Sheet2", "Field A: A3");
    }

    @Test
    void splitsSingleTableRowWhenItExceedsLength() {
        List<TableDocumentParser.TableRow> rows = List.of(
                new TableDocumentParser.TableRow("Sheet1", Map.of("Field A", "abcdef"))
        );

        List<KnowledgeChunkPreview> chunks = splitter.splitTableRows(
                rows,
                new KnowledgeSplitRequest("STRUCTURED_TABLE", 5, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .allSatisfy(content -> assertThat(content.length()).isLessThanOrEqualTo(5));
    }
}
