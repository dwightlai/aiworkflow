package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeDocumentSplitterTest {
    private final KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), new LocalEmbeddingClient());

    @Test
    void splitDocumentRoutesEachConfiguredStrategyToItsOwnAlgorithm() {
        String content = """
                # \u6807\u9898

                \u7b2c\u4e00\u6bb5\u5305\u542b\u4e00\u4e9b\u8f83\u957f\u7684\u6d4b\u8bd5\u5185\u5bb9\u3002\u8fd9\u662f\u7b2c\u4e8c\u53e5\u3002

                \u7b2c\u4e8c\u6bb5\u7528\u4e8e\u9a8c\u8bc1\u6bb5\u843d\u5207\u5206\u3002@@\u7b26\u53f7\u540e\u7684\u5185\u5bb9\u3002
                """;

        List<KnowledgeChunkPreview> structured = splitter.splitDocument(
                "sample.md", content,
                new KnowledgeSplitRequest("STRUCTURE_AWARE", 20, 0, null)
        );
        List<KnowledgeChunkPreview> fixed = splitter.splitDocument(
                "sample.md", content,
                new KnowledgeSplitRequest("FIXED_LENGTH", 20, 0, null)
        );
        List<KnowledgeChunkPreview> paragraphs = splitter.splitDocument(
                "sample.md", content,
                new KnowledgeSplitRequest("PARAGRAPH", 200, 0, null)
        );
        List<KnowledgeChunkPreview> sentences = splitter.splitDocument(
                "sample.md", content,
                new KnowledgeSplitRequest("SENTENCE_BOUNDARY", 20, 0, null)
        );
        List<KnowledgeChunkPreview> symbols = splitter.splitDocument(
                "sample.md", content,
                new KnowledgeSplitRequest("SYMBOL", 200, 0, "@@")
        );

        assertThat(structured).allSatisfy(chunk ->
                assertThat(chunk.sectionPath()).contains("\u6807\u9898"));
        assertThat(fixed).allSatisfy(chunk ->
                assertThat(chunk.tokenEstimate()).isLessThanOrEqualTo(20));
        assertThat(paragraphs).hasSize(3);
        assertThat(sentences).extracting(KnowledgeChunkPreview::content)
                .anyMatch(value -> value.endsWith("\u3002"));
        assertThat(symbols).hasSize(2);
        assertThat(fixed).isNotEqualTo(paragraphs);
        assertThat(paragraphs).isNotEqualTo(symbols);
    }

    @Test
    void splitsTextByFixedLength() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "one two three four",
                new KnowledgeSplitRequest("FIXED_LENGTH", 2, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("one two", "three four");
    }

    @Test
    void allTextStrategiesInterpretChunkSizeAsTokens() {
        String content = "one two three four 中文甲乙";

        for (String strategy : List.of("FIXED_LENGTH", "PARAGRAPH", "SENTENCE_BOUNDARY", "SYMBOL")) {
            KnowledgeSplitRequest request = new KnowledgeSplitRequest(
                    strategy,
                    3,
                    0,
                    "||"
            );
            List<KnowledgeChunkPreview> chunks = splitter.splitText(content, request);

            assertThat(chunks)
                    .as(strategy)
                    .allSatisfy(chunk ->
                            assertThat(chunk.tokenEstimate()).isLessThanOrEqualTo(3));
            assertThat(chunks.get(0).content())
                    .as(strategy + " should use the available token budget")
                    .contains("one two");
        }
    }

    @Test
    void splitsParagraphsAndOnlyCutsParagraphsThatExceedLength() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "short\n\none two three four",
                new KnowledgeSplitRequest("PARAGRAPH", 2, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("short", "one two", "three four");
    }

    @Test
    void splitsTextByLocalSemanticSentenceBoundaries() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "A sentence. Another sentence. Last.",
                new KnowledgeSplitRequest("SENTENCE_BOUNDARY", 5, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("A sentence.", "Another sentence. Last.");
    }

    @Test
    void compatibilityConstructorDoesNotSilentlyUseFakeSemanticEmbeddings() {
        KnowledgeDocumentSplitter compatibilitySplitter =
                new KnowledgeDocumentSplitter(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), new LocalEmbeddingClient());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        compatibilitySplitter.splitText(
                                "主题一。主题二。",
                                new KnowledgeSplitRequest(
                                        "SEMANTIC",
                                        20,
                                        0,
                                        null,
                                        "configured-model"
                                )
                        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EmbeddingClient");
    }

    @Test
    void splitsChineseAtSentenceBoundaries() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "第一句说明。第二句比较长！第三句结束？",
                new KnowledgeSplitRequest("SENTENCE_BOUNDARY", 8, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("第一句说明。", "第二句比较长！", "第三句结束？");
    }

    @Test
    void structureAwareChunksCarryPathAndKeepCodeAtomic() {
        List<KnowledgeChunkPreview> chunks = splitter.splitDocument(
                "api.md",
                "# API 文档\n## 创建接口\n正文说明\n```json\n{\n  \"chunkSize\": 800\n}\n```",
                new KnowledgeSplitRequest("STRUCTURE_AWARE", 200, 20, null)
        );

        assertThat(chunks)
                .filteredOn(chunk -> chunk.chunkType().equals("CODE_BLOCK"))
                .singleElement()
                .satisfies(chunk -> {
                    assertThat(chunk.atomic()).isTrue();
                    assertThat(chunk.sectionPath()).containsExactly("API 文档", "创建接口");
                    assertThat(chunk.content()).contains("\"chunkSize\": 800");
                    assertThat(chunk.embeddingContent()).contains("API 文档", "创建接口");
                });
    }

    @Test
    void fixedLengthSupportsConfiguredOverlap() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "甲乙丙丁戊己庚辛壬癸",
                new KnowledgeSplitRequest("FIXED_LENGTH", 5, 2, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("甲乙丙丁戊", "丁戊己庚辛", "庚辛壬癸");
    }

    @Test
    void splitsTextByCustomSymbolThenCutsOverflow() {
        List<KnowledgeChunkPreview> chunks = splitter.splitText(
                "a|one two three four|gh",
                new KnowledgeSplitRequest("SYMBOL", 2, "|")
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .containsExactly("a", "one two", "three four", "gh");
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
    void keepsSingleTableRowAtomicWhenItExceedsLength() {
        List<TableDocumentParser.TableRow> rows = List.of(
                new TableDocumentParser.TableRow("Sheet1", Map.of("Field A", "abcdef"))
        );

        List<KnowledgeChunkPreview> chunks = splitter.splitTableRows(
                rows,
                new KnowledgeSplitRequest("STRUCTURED_TABLE", 5, null)
        );

        assertThat(chunks).extracting(KnowledgeChunkPreview::content)
                .singleElement()
                .asString()
                .contains("Sheet: Sheet1", "Field A: abcdef");
    }
}
