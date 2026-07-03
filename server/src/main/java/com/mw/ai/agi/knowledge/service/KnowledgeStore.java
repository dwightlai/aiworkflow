package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.KnowledgeDocument;

import java.util.List;
import java.util.Optional;

public interface KnowledgeStore {
    KnowledgeBase saveKnowledgeBase(KnowledgeBase knowledgeBase);

    Optional<KnowledgeBase> findKnowledgeBaseById(String id);

    List<KnowledgeBase> listKnowledgeBases(String tenantId);

    void deleteKnowledgeBase(String id);

    KnowledgeDocument saveDocument(KnowledgeDocument document);

    List<KnowledgeDocument> listDocuments(String knowledgeBaseId);

    void deleteDocuments(String knowledgeBaseId);

    void deleteDocument(String knowledgeBaseId, String documentId);

    KnowledgeChunk saveChunk(KnowledgeChunk chunk);

    List<KnowledgeChunk> listChunks(String knowledgeBaseId, String documentId);

    List<KnowledgeChunk> listChunks(String knowledgeBaseId);

    void deleteChunks(String knowledgeBaseId);

    void deleteChunks(String knowledgeBaseId, String documentId);

    void deleteChunk(String chunkId);

    KnowledgeChunkVector saveChunkVector(KnowledgeChunkVector vector);

    List<KnowledgeChunkVector> listChunkVectors(String knowledgeBaseId);

    void deleteChunkVectors(String knowledgeBaseId);

    void deleteChunkVectors(String knowledgeBaseId, String documentId);
}
