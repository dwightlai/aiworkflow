package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocxEmbeddedTableChunkingTest {
    @Test
    void keepsSmallEmbeddedDocumentClassificationTableAsOneChunk() throws Exception {
        Path root = Path.of(
                "..", "sampledata", "chunkSample", "business_chunking_dataset_500", "docx"
        );
        Path document;
        try (var paths = Files.walk(root)) {
            document = paths
                    .filter(path -> path.getFileName().toString().startsWith("DOCX_0166_"))
                    .findFirst()
                    .orElseThrow();
        }
        DocumentTextExtractor extractor = new DocumentTextExtractor();
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), new LocalEmbeddingClient());

        String content;
        try (InputStream input = Files.newInputStream(document)) {
            content = extractor.extract(document.getFileName().toString(), null, input);
        }
        List<KnowledgeChunkPreview> chunks = splitter.splitDocument(
                document.getFileName().toString(),
                content,
                new KnowledgeSplitRequest("STRUCTURE_AWARE", 800, 100, null)
        );

        List<KnowledgeChunkPreview> uploadTable = chunks.stream()
                .filter(chunk -> chunk.sectionPath().contains("\u4e8c\u3001\u4e0a\u4f20\u4e1a\u52a1\u6587\u6863"))
                .filter(chunk -> chunk.chunkType().startsWith("TABLE"))
                .toList();

        assertThat(uploadTable).singleElement().satisfies(chunk -> {
            assertThat(chunk.chunkType()).isEqualTo("TABLE");
            assertThat(chunk.atomic()).isTrue();
            assertThat(chunk.content()).contains(
                    "\u6587\u4ef6\u7c7b\u578b: DOCX",
                    "\u6587\u4ef6\u7c7b\u578b: PDF",
                    "\u6587\u4ef6\u7c7b\u578b: XLSX",
                    "\u63a8\u8350\u5206\u5757\u65b9\u5f0f"
            );
        });
        assertThat(chunks)
                .filteredOn(chunk -> chunk.sectionPath().contains("\u4e09\u3001\u91cd\u65b0\u89e3\u6790"))
                .allSatisfy(chunk -> assertThat(chunk.chunkType()).isNotEqualTo("TABLE"));
    }
}
