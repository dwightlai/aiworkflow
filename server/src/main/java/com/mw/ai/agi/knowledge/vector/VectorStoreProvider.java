package com.mw.ai.agi.knowledge.vector;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;

import java.util.List;

public interface VectorStoreProvider {
    String storeType();
    VectorStoreConnectionResult testConnection(VectorStoreConfig config);
    void ensureStore(VectorStoreConfig config, int dimensions);
    void upsertChunk(VectorStoreConfig config, KnowledgeChunk chunk, KnowledgeChunkVector vector);
    List<VectorStoreSearchHit> search(VectorStoreConfig config, VectorStoreSearchRequest request);
    void deleteDocument(VectorStoreConfig config, String knowledgeBaseId, String documentId);
    void deleteKnowledgeBase(VectorStoreConfig config, String knowledgeBaseId);
    default void close(VectorStoreConfig config) {}
}
