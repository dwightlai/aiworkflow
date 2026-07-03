package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessOcrPdfChunkingTest {
    private static final Path ROOT = Path.of(
            "..",
            "sampledata",
            "chunkSample",
            "business_chunking_dataset_500",
            "pdf",
            "扫描OCR类"
    );

    @Test
    void preservesOcrConfidenceThresholdInAllRegressionPdfs() throws Exception {
        DocumentTextExtractor extractor = new DocumentTextExtractor();
        KnowledgeDocumentSplitter splitter =
                new KnowledgeDocumentSplitter(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), new LocalEmbeddingClient());

        for (String fileName : List.of(
                "PDF_0421_扫描OCR类.pdf",
                "PDF_0425_扫描OCR类.pdf",
                "PDF_0430_扫描OCR类.pdf"
        )) {
            Path file = ROOT.resolve(fileName);
            String content;
            try (InputStream input = Files.newInputStream(file)) {
                content = extractor.extract(fileName, "application/pdf", input);
            }
            List<KnowledgeChunkPreview> chunks = splitter.splitDocument(
                    fileName,
                    content,
                    new KnowledgeSplitRequest("STRUCTURE_AWARE", 500, 50, null)
            );

            assertThat(content).as(fileName + " extracted text").contains("0.85");
            assertThat(chunks)
                    .as(fileName + " chunks")
                    .anySatisfy(chunk -> assertThat(chunk.content()).contains("0.85"));
        }
    }
}
