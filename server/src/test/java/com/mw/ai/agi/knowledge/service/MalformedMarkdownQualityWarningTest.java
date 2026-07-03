package com.mw.ai.agi.knowledge.service;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MalformedMarkdownQualityWarningTest {
    @Test
    void warnsWhenMarkdownContainsManyInlineHeadingsInOnePhysicalLine() throws Exception {
        Path document = Path.of(
                "..", "sampledata", "chunkSample",
                "\u7535\u5b50\u6863\u6848\u56db\u6027\u68c0\u6d4b\u4e13\u9898\u7814\u7a76\u62a5\u544a.md"
        );
        KnowledgeBaseService service = new KnowledgeBaseService();

        KnowledgeBaseService.UploadedDocumentPreview preview;
        try (InputStream input = Files.newInputStream(document)) {
            preview = service.previewTextDocumentFile(
                    document.getFileName().toString(),
                    "text/markdown",
                    input,
                    new KnowledgeSplitRequest("STRUCTURE_AWARE", 500, 50, null)
            );
        }

        assertThat(preview.warnings())
                .anySatisfy(warning -> assertThat(warning)
                        .contains("\u8d85\u957f\u5355\u884c", "Markdown", "\u524d\u7f6e\u89e3\u6790\u6e05\u6d17"));
    }
}
