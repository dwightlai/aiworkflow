package com.mw.ai.agi.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelTableUploadChunkingTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void tableUploadKeepsOneCompleteBusinessRowWithItsRealHeaderForEveryProcurementWorkbook() throws Exception {
        Path directory = Path.of(
                "..", "sampledata", "chunkSample", "business_chunking_dataset_500",
                "xlsx", "\u91c7\u8d2d\u53f0\u8d26\u7c7b"
        );
        KnowledgeBaseService service = new KnowledgeBaseService();

        List<Path> workbooks;
        try (var paths = Files.list(directory)) {
            workbooks = paths
                    .filter(path -> path.toString().endsWith(".xlsx"))
                    .filter(path -> !path.getFileName().toString().startsWith("~$"))
                    .sorted()
                    .toList();
        }
        assertThat(workbooks).hasSize(24);

        for (Path workbook : workbooks) {
            KnowledgeBaseService.UploadedDocumentPreview preview;
            try (InputStream input = Files.newInputStream(workbook)) {
                preview = service.previewTableDocumentFile(
                        workbook.getFileName().toString(),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        input,
                        new KnowledgeSplitRequest("STRUCTURED_TABLE", 500, null)
                );
            }

            assertThat(preview.chunks())
                    .as(workbook.getFileName().toString())
                    .hasSize(12)
                    .allSatisfy(chunk -> {
                        assertThat(chunk.chunkType()).isEqualTo("TABLE_ROW");
                        assertThat(chunk.atomic()).isTrue();
                        assertThat(chunk.content()).doesNotContain("---", "Column 2", "Column 3", "Sheet:");
                        assertThat(chunk.content()).doesNotContain("|");
                        assertThat(chunk.content()).contains(
                                "\u91c7\u8d2d\u7f16\u53f7:",
                                "\u7533\u8bf7\u90e8\u95e8:",
                                "\u4f9b\u5e94\u5546:",
                                "\u5ba1\u6279\u72b6\u6001:"
                        );
                        assertThat(chunk.embeddingContent())
                                .doesNotContain("---", "|", "\u5b57\u6bb5\u5217\u8868")
                                .contains("\u91c7\u8d2d\u53f0\u8d26\u8bb0\u5f55");
                        JsonNode metadata;
                        try {
                            metadata = objectMapper.readTree(chunk.metadataJson());
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                        assertThat(metadata.path("sourceFile").asText()).isEqualTo(
                                workbook.getFileName().toString()
                        );
                        assertThat(metadata.path("sheetName").asText()).isEqualTo("\u91c7\u8d2d\u53f0\u8d26");
                        assertThat(metadata.path("rowIndex").asInt()).isGreaterThanOrEqualTo(1);
                        assertThat(metadata.path("schema").isArray()).isTrue();
                        assertThat(metadata.path("data").path("\u91c7\u8d2d\u7f16\u53f7").asText())
                                .startsWith("CG-2026-");
                        assertThat(metadata.path("fieldTypes").path("\u9884\u7b97\u91d1\u989d").asText())
                                .isEqualTo("NUMBER");
                    });
            assertThat(preview.chunks().stream().map(KnowledgeChunkPreview::content))
                    .anySatisfy(content -> assertThat(content).contains("CG-2026-"));
        }
    }
}
