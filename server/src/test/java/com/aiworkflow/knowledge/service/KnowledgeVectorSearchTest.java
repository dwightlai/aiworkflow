package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeVectorSearchTest {
    @Test
    void storesEmbeddingsAndUsesVectorSearchWhenKnowledgeBaseIsConfiguredForVectorRetrieval() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(
                new KnowledgeSplitter(),
                store,
                new LocalEmbeddingClient()
        );
        KnowledgeBase knowledgeBase = service.create(
                "support",
                null,
                "embedding-model-1",
                "vector-store-1",
                "SIMPLE_TEXT",
                500,
                0,
                "VECTOR",
                3
        );

        KnowledgeDocument document = service.addDocument(
                knowledgeBase.id(),
                "refund.txt",
                "Refund requests require the original package."
        );

        assertThat(store.listChunkVectors(knowledgeBase.id()))
                .hasSize(1)
                .first()
                .satisfies(vector -> {
                    assertThat(vector.documentId()).isEqualTo(document.id());
                    assertThat(vector.embedding()).hasSize(LocalEmbeddingClient.DIMENSIONS);
                });
        assertThat(service.search(knowledgeBase.id(), "refund package", 3))
                .hasSize(1)
                .first()
                .extracting("documentName")
                .isEqualTo("refund.txt");
    }

    @Test
    void deletesChunkEmbeddingsWithDocuments() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(
                new KnowledgeSplitter(),
                store,
                new LocalEmbeddingClient()
        );
        KnowledgeBase knowledgeBase = service.create("support", null, "embedding-model-1", null, "SIMPLE_TEXT", 500, 0, "HYBRID", 3);
        KnowledgeDocument document = service.addDocument(knowledgeBase.id(), "faq.txt", "Invoices can be downloaded after payment.");

        service.deleteDocument(knowledgeBase.id(), document.id());

        assertThat(store.listChunkVectors(knowledgeBase.id())).isEmpty();
    }
}
