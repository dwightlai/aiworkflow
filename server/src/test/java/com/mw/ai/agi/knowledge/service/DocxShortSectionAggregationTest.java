package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocxShortSectionAggregationTest {
    @Test
    void keepsNumberedStepsTogetherWithinOneManualSection() throws Exception {
        Path root = Path.of(
                "..", "sampledata", "chunkSample", "business_chunking_dataset_500", "docx"
        );
        Path document;
        try (var paths = Files.walk(root)) {
            document = paths
                    .filter(path -> path.getFileName().toString().startsWith("DOCX_0167_"))
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
                new KnowledgeSplitRequest("STRUCTURE_AWARE", 500, 50, null)
        );

        List<KnowledgeChunkPreview> createKnowledgeBase = chunks.stream()
                .filter(chunk -> chunk.sectionPath().contains("\u4e00\u3001\u521b\u5efa\u77e5\u8bc6\u5e93"))
                .toList();
        assertThat(createKnowledgeBase).singleElement().satisfies(chunk -> {
            assertThat(chunk.content()).contains(
                    "1. \u8fdb\u5165\u77e5\u8bc6\u5e93\u7ba1\u7406\u9875\u9762",
                    "2. \u70b9\u51fb\u65b0\u5efa\u77e5\u8bc6\u5e93",
                    "3. \u586b\u5199\u540d\u79f0\u3001\u63cf\u8ff0\u548c\u6240\u5c5e\u5e94\u7528",
                    "4. \u9009\u62e9\u9ed8\u8ba4 Embedding \u6a21\u578b",
                    "5. \u4fdd\u5b58\u5e76\u8fdb\u5165\u6587\u6863\u4e0a\u4f20\u9875\u9762"
            );
            assertThat(chunk.tokenEstimate()).isGreaterThan(15);
        });
    }
}
