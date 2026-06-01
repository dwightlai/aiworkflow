package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeChunkVector;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;

import java.util.List;
import java.util.Optional;

public interface KnowledgeStore {
    KnowledgeBase saveKnowledgeBase(KnowledgeBase knowledgeBase);

    Optional<KnowledgeBase> findKnowledgeBaseById(String id);

    List<KnowledgeBase> listKnowledgeBases();

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

    KnowledgeChunkVector saveChunkVector(KnowledgeChunkVector vector);

    List<KnowledgeChunkVector> listChunkVectors(String knowledgeBaseId);

    void deleteChunkVectors(String knowledgeBaseId);

    void deleteChunkVectors(String knowledgeBaseId, String documentId);
}
