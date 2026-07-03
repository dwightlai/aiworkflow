package com.mw.ai.agi.knowledge.vector;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.mw.ai.agi.knowledge.service.ElasticsearchVectorStoreClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ElasticsearchVectorStoreProvider implements VectorStoreProvider {
    private final ElasticsearchVectorStoreClient client;

    public ElasticsearchVectorStoreProvider(ElasticsearchVectorStoreClient client) {
        this.client = client;
    }

    @Override
    public String storeType() {
        return "ELASTICSEARCH";
    }

    @Override
    public VectorStoreConnectionResult testConnection(VectorStoreConfig config) {
        Instant started = Instant.now();
        client.ensureIndex(config, config.vectorDimension());
        return new VectorStoreConnectionResult(
                true, storeType(), null,
                Duration.between(started, Instant.now()).toMillis(), "Connection successful"
        );
    }

    @Override
    public void ensureStore(VectorStoreConfig config, int dimensions) {
        client.ensureIndex(config, dimensions);
    }

    @Override
    public void upsertChunk(VectorStoreConfig config, KnowledgeChunk chunk, KnowledgeChunkVector vector) {
        client.upsertChunk(config, chunk, vector);
    }

    @Override
    public List<VectorStoreSearchHit> search(VectorStoreConfig config, VectorStoreSearchRequest request) {
        return client.search(
                        config, request.knowledgeBaseId(), request.datasetId(),
                        request.queryEmbedding(), request.topK()
                ).stream()
                .map(hit -> new VectorStoreSearchHit(
                        hit.chunkId(), hit.documentId(), hit.documentName(), hit.content(),
                        request.datasetId(), hit.score(), "{}"
                ))
                .filter(hit -> hit.score() >= request.similarityThreshold())
                .toList();
    }

    @Override
    public void deleteDocument(VectorStoreConfig config, String knowledgeBaseId, String documentId) {
        client.deleteDocument(config, knowledgeBaseId, documentId);
    }

    @Override
    public void deleteKnowledgeBase(VectorStoreConfig config, String knowledgeBaseId) {
        client.deleteKnowledgeBase(config, knowledgeBaseId);
    }
}
