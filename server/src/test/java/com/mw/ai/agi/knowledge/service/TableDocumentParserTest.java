package com.mw.ai.agi.knowledge.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TableDocumentParserTest {
    private final TableDocumentParser parser = new TableDocumentParser();

    @Test
    void parsesDelimitedTableTextIntoRowsAndFiltersEmptyRows() {
        String content = """
                Name\tAge
                Alice\t18

                Bob\t20
                \t
                """;

        assertThat(parser.parseText(content, "Sheet1"))
                .hasSize(2)
                .first()
                .satisfies(row -> {
                    assertThat(row.sheetName()).isEqualTo("Sheet1");
                    assertThat(row.cells()).containsEntry("Name", "Alice");
                    assertThat(row.cells()).containsEntry("Age", "18");
                });
    }
}
