package com.mw.ai.agi.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.config.AgiStorageProperties;
import com.mw.ai.agi.config.AgiStorageSettingsService;
import com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.mw.ai.agi.knowledge.vector.VectorStoreConnectionResult;
import com.mw.ai.agi.knowledge.vector.VectorStoreProvider;
import com.mw.ai.agi.knowledge.vector.VectorStoreProviderRegistry;
import com.mw.ai.agi.knowledge.vector.VectorStoreSearchHit;
import com.mw.ai.agi.knowledge.vector.VectorStoreSearchRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeBaseVectorStoreRoutingTest {
    @Test
    void routesWriteSearchAndDeleteToSelectedPgvectorProvider() {
        InMemoryKnowledgeStore knowledgeStore = new InMemoryKnowledgeStore();
        InMemoryVectorStoreConfigStore configStore = new InMemoryVectorStoreConfigStore();
        RecordingProvider provider = new RecordingProvider();
        VectorStoreConfig config = configStore.save(new VectorStoreConfig(
                "vector_pg", "Pgvector", "PGVECTOR", null, "agi_vectors",
                "127.0.0.1", 5432, "vectors", "agi_vectors", LocalEmbeddingClient.DIMENSIONS,
                false, "{}", "user", "secret", null,
                5000, 30000, true, Instant.now(), Instant.now()
        ));
        com.mw.ai.agi.knowledge.chunking.TokenCounter tokenCounter = new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter();
        KnowledgeBaseService service = new KnowledgeBaseService(
                tokenCounter,
                knowledgeStore,
                new LocalEmbeddingClient(),
                new DocumentTextExtractor(),
                new KnowledgeDocumentSplitter(tokenCounter, new LocalEmbeddingClient()),
                new TableDocumentParser(),
                configStore,
                new VectorStoreProviderRegistry(List.of(provider)),
                null,
                null,
                new HeuristicKnowledgeReranker(),
                new KnowledgeContextAssembler(new HeuristicTokenCounter()),
                new KnowledgeDocumentFileStorage(AgiStorageSettingsService.withDefaults(
                        new AgiStorageProperties(), new ObjectMapper()
                ))
        );
        KnowledgeBase knowledgeBase = service.create(
                "routing", null, null, "embedding", config.id(), LocalEmbeddingClient.DIMENSIONS,
                "SIMPLE_TEXT", 500, 0, "VECTOR", 3
        );
        var document = service.addDocument(knowledgeBase.id(), "routing.txt", "Provider routing content");

        assertThat(provider.upsertCount).isEqualTo(1);
        assertThat(service.search(knowledgeBase.id(), "routing", 3)).hasSize(1);
        assertThat(provider.searchCount).isEqualTo(1);

        service.deleteDocument(knowledgeBase.id(), document.id());
        assertThat(provider.deleteDocumentCount).isEqualTo(1);
        service.delete(knowledgeBase.id());
        assertThat(provider.deleteKnowledgeBaseCount).isEqualTo(1);
    }

    private static final class RecordingProvider implements VectorStoreProvider {
        int upsertCount;
        int searchCount;
        int deleteDocumentCount;
        int deleteKnowledgeBaseCount;
        KnowledgeChunk lastChunk;

        @Override public String storeType() { return "PGVECTOR"; }
        @Override public VectorStoreConnectionResult testConnection(VectorStoreConfig config) {
            return new VectorStoreConnectionResult(true, storeType(), null, 0, "OK");
        }
        @Override public void ensureStore(VectorStoreConfig config, int dimensions) {}
        @Override public void upsertChunk(
                VectorStoreConfig config, KnowledgeChunk chunk, KnowledgeChunkVector vector
        ) {
            upsertCount++;
            lastChunk = chunk;
        }
        @Override public List<VectorStoreSearchHit> search(
                VectorStoreConfig config, VectorStoreSearchRequest request
        ) {
            searchCount++;
            return List.of(new VectorStoreSearchHit(
                    lastChunk.id(), lastChunk.documentId(), lastChunk.documentName(),
                    lastChunk.content(), lastChunk.datasetId(), 0.9, "{}"
            ));
        }
        @Override public void deleteDocument(
                VectorStoreConfig config, String knowledgeBaseId, String documentId
        ) {
            deleteDocumentCount++;
        }
        @Override public void deleteKnowledgeBase(VectorStoreConfig config, String knowledgeBaseId) {
            deleteKnowledgeBaseCount++;
        }
    }
}
