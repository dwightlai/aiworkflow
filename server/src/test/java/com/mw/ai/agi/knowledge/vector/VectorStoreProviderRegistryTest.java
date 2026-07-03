package com.mw.ai.agi.knowledge.vector;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VectorStoreProviderRegistryTest {
    @Test
    void resolvesProvidersCaseInsensitivelyAndRejectsUnsupportedTypes() {
        VectorStoreProvider milvus = new StubProvider("MILVUS");
        VectorStoreProviderRegistry registry = new VectorStoreProviderRegistry(List.of(milvus));

        assertThat(registry.require("milvus")).isSameAs(milvus);
        assertThatThrownBy(() -> registry.require("qdrant"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("QDRANT");
    }

    private record StubProvider(String storeType) implements VectorStoreProvider {
        @Override public VectorStoreConnectionResult testConnection(VectorStoreConfig config) {
            return new VectorStoreConnectionResult(true, storeType, null, 0, "OK");
        }
        @Override public void ensureStore(VectorStoreConfig config, int dimensions) {}
        @Override public void upsertChunk(VectorStoreConfig config, KnowledgeChunk chunk, KnowledgeChunkVector vector) {}
        @Override public List<VectorStoreSearchHit> search(VectorStoreConfig config, VectorStoreSearchRequest request) {
            return List.of();
        }
        @Override public void deleteDocument(VectorStoreConfig config, String knowledgeBaseId, String documentId) {}
        @Override public void deleteKnowledgeBase(VectorStoreConfig config, String knowledgeBaseId) {}
        @Override public void close(VectorStoreConfig config) {}
    }
}
