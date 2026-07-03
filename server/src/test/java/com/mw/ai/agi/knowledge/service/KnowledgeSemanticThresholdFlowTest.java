package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeSemanticThresholdFlowTest {

    @Test
    void uploadedDocumentUsesKnowledgeBaseThresholdWhenRequestDoesNotOverrideIt() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(new HeuristicTokenCounter(), store);
        KnowledgeBase knowledgeBase = service.create(
                "semantic-kb", null, null, "embedding-test", null, 1024,
                "SEMANTIC", 200, 20, "VECTOR", 5, "NORMAL", "MULTI", 0.63
        );

        KnowledgeDocument document = service.addTextDocumentFile(
                knowledgeBase.id(),
                "sample.txt",
                "text/plain",
                stream("First topic.\n\nSecond topic."),
                new KnowledgeSplitRequest("SEMANTIC", 200, 20, null, "embedding-test"),
                null
        );

        assertThat(document.splitterConfig())
                .contains("\"semanticSimilarityThreshold\":0.63");
    }

    @Test
    void uploadedDocumentCanOverrideKnowledgeBaseThreshold() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(new HeuristicTokenCounter(), store);
        KnowledgeBase knowledgeBase = service.create(
                "semantic-kb", null, null, "embedding-test", null, 1024,
                "SEMANTIC", 200, 20, "VECTOR", 5, "NORMAL", "MULTI", 0.63
        );

        KnowledgeDocument document = service.addTextDocumentFile(
                knowledgeBase.id(),
                "sample.txt",
                "text/plain",
                stream("First topic.\n\nSecond topic."),
                new KnowledgeSplitRequest("SEMANTIC", 200, 20, null, "embedding-test", 0.91),
                null
        );

        assertThat(document.splitterConfig())
                .contains("\"semanticSimilarityThreshold\":0.91");
    }

    @Test
    void semanticPreviewResolvesEmbeddingModelAndThresholdFromKnowledgeBase() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(new HeuristicTokenCounter(), store);
        KnowledgeBase knowledgeBase = service.create(
                "semantic-kb", null, null, "embedding-test", null, 1024,
                "SEMANTIC", 200, 20, "VECTOR", 5, "NORMAL", "MULTI", 0.63
        );

        KnowledgeBaseService.UploadedDocumentPreview preview = service.previewTextDocumentFile(
                "sample.txt",
                "text/plain",
                stream("First topic.\n\nSecond topic."),
                new KnowledgeSplitRequest("SEMANTIC", 200, 20, null),
                knowledgeBase.id()
        );

        assertThat(preview.chunks()).isNotEmpty();
    }

    private ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
