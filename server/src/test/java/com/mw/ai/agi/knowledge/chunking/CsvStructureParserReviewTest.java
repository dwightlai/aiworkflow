package com.mw.ai.agi.knowledge.chunking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvStructureParserReviewTest {

    @Test
    void parsesQuotedCommasEscapedQuotesAndMultilineFields() {
        String csv = "id,title,description\n"
                + "1,\"采购,验收\",\"第一行\n第二行\"\n"
                + "2,\"合同\"\"归档\"\"\",完成\n";

        DocumentStructure document = new CsvStructureParser().parse("records.csv", csv);

        assertThat(document.nodes()).hasSize(2);
        assertThat(document.nodes().get(0).metadata())
                .containsEntry("title", "采购,验收")
                .containsEntry("description", "第一行\n第二行");
        assertThat(document.nodes().get(1).metadata())
                .containsEntry("title", "合同\"归档\"");
    }
}
