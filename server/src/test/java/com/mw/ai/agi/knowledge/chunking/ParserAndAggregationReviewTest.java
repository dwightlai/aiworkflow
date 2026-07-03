package com.mw.ai.agi.knowledge.chunking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParserAndAggregationReviewTest {
    private final TokenCounter characterTokens = (text, modelId) ->
            text == null ? 0 : text.replaceAll("\\s+", "").length();

    @Test
    void officeParserMarksPlainExtractionAsDegraded() {
        DocumentStructure document = new OfficeStructureParser().parse(
                "manual.docx",
                "第一段操作说明。\n\n第二段操作说明。"
        );

        assertThat(document.metadata())
                .containsEntry("structurePreserved", false)
                .containsEntry("parserFallback", "PLAIN_TEXT");
        assertThat(document.nodes()).hasSize(2);
    }

    @Test
    void officeTableRowsUseLogicalDataRowNumbers() {
        DocumentStructure document = new OfficeStructureParser().parse(
                "ledger.xlsx",
                """
                # 工作表：采购台账

                | 编号 | 状态 |
                | --- | --- |
                | CG-01 | 已完成 |
                | CG-02 | 待审批 |
                """
        );

        assertThat(document.nodes())
                .filteredOn(node -> node.type() == NodeType.TABLE_ROW)
                .extracting(node -> node.metadata().get("rowIndex"))
                .containsExactly(1, 2);
    }

    @Test
    void plainTextShortParagraphsAggregateWithinDocumentContext() {
        DocumentStructure document = new PlainTextStructureParser().parse(
                "notice.txt",
                "第一项。\n\n第二项。\n\n第三项。"
        );

        List<KnowledgeChunkPreview> chunks = new DefaultChunkStrategyRouter(characterTokens)
                .split(document, ChunkProfile.defaults("STRUCTURE_AWARE", 100, 0));

        assertThat(chunks).singleElement().satisfies(chunk ->
                assertThat(chunk.content()).contains("第一项。", "第二项。", "第三项。"));
    }

    @Test
    void aggregatedChunksRetainSourceMetadata() throws Exception {
        DocumentStructure document = new DocumentStructure(
                "制度",
                "MD",
                "",
                List.of(
                        paragraph("a", 0, Map.of("page", 1)),
                        paragraph("b", 1, Map.of("page", 2))
                ),
                Map.of()
        );

        KnowledgeChunkPreview chunk = new DefaultChunkStrategyRouter(characterTokens)
                .split(document, ChunkProfile.defaults("STRUCTURE_AWARE", 20, 0))
                .get(0);
        Map<String, Object> metadata = new ObjectMapper().readValue(
                chunk.metadataJson(),
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                }
        );

        assertThat(metadata).containsKey("sourceMetadata");
        assertThat((List<?>) metadata.get("sourceMetadata")).hasSize(2);
    }

    private DocumentNode paragraph(
            String text,
            int order,
            Map<String, Object> metadata
    ) {
        return new DocumentNode(
                "node-" + order,
                NodeType.PARAGRAPH,
                text,
                List.of("第一章"),
                null,
                null,
                order,
                null,
                metadata,
                List.of()
        );
    }
}
