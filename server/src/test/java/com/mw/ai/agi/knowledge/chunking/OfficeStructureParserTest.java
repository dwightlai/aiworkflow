package com.mw.ai.agi.knowledge.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OfficeStructureParserTest {
    private final OfficeStructureParser parser = new OfficeStructureParser();

    @Test
    void expandsNormalizedSpreadsheetRowsIntoStructuredAtomicRecords() {
        DocumentStructure document = parser.parse(
                "\u91c7\u8d2d\u53f0\u8d26.xlsx",
                """
                # \u5de5\u4f5c\u8868\uff1a\u91c7\u8d2d\u53f0\u8d26

                | \u91c7\u8d2d\u7f16\u53f7 | \u7533\u8bf7\u90e8\u95e8 | \u9884\u7b97\u91d1\u989d | \u5ba1\u6279\u72b6\u6001 | \u5907\u6ce8 |
                | --- | --- | --- | --- | --- |
                | CG-2026-0301-01 | \u4ea4\u4ed8\u90e8 | \u00a5840,000 | \u5df2\u9a73\u56de | |
                """
        );

        assertThat(document.nodes()).filteredOn(node -> node.type() == NodeType.TABLE_ROW)
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.text()).isEqualTo("""
                            \u91c7\u8d2d\u7f16\u53f7: CG-2026-0301-01
                            \u7533\u8bf7\u90e8\u95e8: \u4ea4\u4ed8\u90e8
                            \u9884\u7b97\u91d1\u989d: \u00a5840,000
                            \u5ba1\u6279\u72b6\u6001: \u5df2\u9a73\u56de""");
                    assertThat(node.metadata())
                            .containsEntry("atomic", true)
                            .containsEntry("sheetName", "\u91c7\u8d2d\u53f0\u8d26")
                            .containsEntry("rowIndex", 2);
                    assertThat(node.metadata().get("schema")).isEqualTo(List.of(
                            "\u91c7\u8d2d\u7f16\u53f7", "\u7533\u8bf7\u90e8\u95e8",
                            "\u9884\u7b97\u91d1\u989d", "\u5ba1\u6279\u72b6\u6001", "\u5907\u6ce8"
                    ));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) node.metadata().get("data");
                    assertThat(data)
                            .containsEntry("\u91c7\u8d2d\u7f16\u53f7", "CG-2026-0301-01")
                            .containsEntry("\u7533\u8bf7\u90e8\u95e8", "\u4ea4\u4ed8\u90e8")
                            .containsEntry("\u9884\u7b97\u91d1\u989d", 840000L)
                            .containsEntry("\u5907\u6ce8", null);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldTypes =
                            (Map<String, Object>) node.metadata().get("fieldTypes");
                    assertThat(fieldTypes)
                            .containsEntry("\u91c7\u8d2d\u7f16\u53f7", "STRING")
                            .containsEntry("\u9884\u7b97\u91d1\u989d", "NUMBER")
                            .containsEntry("\u5907\u6ce8", "NULL");
                });
    }

    @Test
    void keepsHeadingPathForDocxParagraphs() {
        DocumentStructure document = parser.parse(
                "\u5408\u540c.docx",
                "# \u8f6f\u4ef6\u670d\u52a1\u91c7\u8d2d\u5408\u540c\n\n## \u4e09\u3001\u4ed8\u6b3e\u8282\u70b9\n\n\u4e0a\u7ebf\u6b3e\u4e3a 40%\u3002"
        );

        assertThat(document.nodes()).anySatisfy(node -> {
            if (node.type() == NodeType.PARAGRAPH) {
                assertThat(node.sectionPath()).containsExactly(
                        "\u8f6f\u4ef6\u670d\u52a1\u91c7\u8d2d\u5408\u540c",
                        "\u4e09\u3001\u4ed8\u6b3e\u8282\u70b9"
                );
            }
        });
    }
}
