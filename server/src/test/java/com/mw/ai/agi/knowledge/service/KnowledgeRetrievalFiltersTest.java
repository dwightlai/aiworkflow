package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeRetrievalFiltersTest {
    @Test
    void filtersChunksByMaterialType() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), store);
        KnowledgeBase knowledgeBase = service.create("kb", "desc", null, null, null, "SIMPLE_TEXT", 500, 0, "HYBRID", 3);
        service.addDocument(knowledgeBase.id(), "a.txt", "档案资料正文");
        KnowledgeChunk original = store.listChunks(knowledgeBase.id()).get(0);
        KnowledgeChunk chunk = original.withDatasetContext(
                original.datasetId(), null, null, null, "ARCHIVE_FILE", "PROJECT_DOCUMENT", "ARCH_1");
        store.saveChunk(chunk);

        List<KnowledgeSearchResult> results = service.search(
                knowledgeBase.id(),
                null,
                "档案",
                5,
                Map.of(),
                KnowledgeSearchOptions.of("KEYWORD", Map.of("materialType", List.of("PROJECT_DOCUMENT")))
        );
        assertThat(results).hasSize(1);

        List<KnowledgeSearchResult> empty = service.search(
                knowledgeBase.id(),
                null,
                "档案",
                5,
                Map.of(),
                KnowledgeSearchOptions.of("KEYWORD", Map.of("materialType", List.of("TOPIC_NOTE")))
        );
        assertThat(empty).isEmpty();
    }

    @Test
    void requestRetrievalModeOverridesKnowledgeBaseConfiguration() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(
                new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(),
                store,
                new LocalEmbeddingClient()
        );
        KnowledgeBase knowledgeBase = service.create(
                "support",
                null,
                null,
                "embedding-model-1",
                null,
                "SIMPLE_TEXT",
                500,
                0,
                "VECTOR",
                3
        );
        service.addDocument(knowledgeBase.id(), "faq.txt", "发票可以在订单完成后七日内申请。");

        assertThat(service.search(
                knowledgeBase.id(),
                null,
                "发票",
                3,
                Map.of(),
                KnowledgeSearchOptions.of("KEYWORD", null)
        )).hasSize(1);
    }

    @Test
    void rejectsUnsupportedRetrievalMode() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), store);
        KnowledgeBase knowledgeBase = service.create("kb", "desc", null, null, null, "SIMPLE_TEXT", 500, 0, "HYBRID", 3);
        service.addDocument(knowledgeBase.id(), "a.txt", "hello world");

        assertThatThrownBy(() -> service.search(
                knowledgeBase.id(),
                null,
                "hello",
                3,
                Map.of(),
                KnowledgeSearchOptions.of("BM25", null)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
