package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeChunkVector;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryKnowledgeStore implements KnowledgeStore {
    private final List<KnowledgeBase> knowledgeBases = new CopyOnWriteArrayList<>();
    private final List<KnowledgeDocument> documents = new CopyOnWriteArrayList<>();
    private final List<KnowledgeChunk> chunks = new CopyOnWriteArrayList<>();
    private final List<KnowledgeChunkVector> chunkVectors = new CopyOnWriteArrayList<>();

    @Override
    public KnowledgeBase saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        deleteKnowledgeBaseOnly(knowledgeBase.id());
        knowledgeBases.add(knowledgeBase);
        return knowledgeBase;
    }

    @Override
    public Optional<KnowledgeBase> findKnowledgeBaseById(String id) {
        return knowledgeBases.stream()
                .filter(knowledgeBase -> knowledgeBase.id().equals(id))
                .findFirst();
    }

    @Override
    public List<KnowledgeBase> listKnowledgeBases() {
        return new ArrayList<>(knowledgeBases);
    }

    @Override
    public void deleteKnowledgeBase(String id) {
        deleteKnowledgeBaseOnly(id);
        deleteDocuments(id);
        deleteChunks(id);
        deleteChunkVectors(id);
    }

    @Override
    public KnowledgeDocument saveDocument(KnowledgeDocument document) {
        documents.removeIf(current -> current.id().equals(document.id()));
        documents.add(document);
        return document;
    }

    @Override
    public List<KnowledgeDocument> listDocuments(String knowledgeBaseId) {
        return documents.stream()
                .filter(document -> document.knowledgeBaseId().equals(knowledgeBaseId))
                .toList();
    }

    @Override
    public void deleteDocuments(String knowledgeBaseId) {
        documents.removeIf(document -> document.knowledgeBaseId().equals(knowledgeBaseId));
    }

    @Override
    public void deleteDocument(String knowledgeBaseId, String documentId) {
        documents.removeIf(document -> document.knowledgeBaseId().equals(knowledgeBaseId) && document.id().equals(documentId));
    }

    @Override
    public KnowledgeChunk saveChunk(KnowledgeChunk chunk) {
        chunks.removeIf(current -> current.id().equals(chunk.id()));
        chunks.add(chunk);
        return chunk;
    }

    @Override
    public List<KnowledgeChunk> listChunks(String knowledgeBaseId, String documentId) {
        return chunks.stream()
                .filter(chunk -> chunk.knowledgeBaseId().equals(knowledgeBaseId))
                .filter(chunk -> chunk.documentId().equals(documentId))
                .sorted(Comparator.comparingInt(KnowledgeChunk::index))
                .toList();
    }

    @Override
    public List<KnowledgeChunk> listChunks(String knowledgeBaseId) {
        return chunks.stream()
                .filter(chunk -> chunk.knowledgeBaseId().equals(knowledgeBaseId))
                .sorted(Comparator.comparing(KnowledgeChunk::documentName).thenComparingInt(KnowledgeChunk::index))
                .toList();
    }

    @Override
    public void deleteChunks(String knowledgeBaseId) {
        chunks.removeIf(chunk -> chunk.knowledgeBaseId().equals(knowledgeBaseId));
    }

    @Override
    public void deleteChunks(String knowledgeBaseId, String documentId) {
        chunks.removeIf(chunk -> chunk.knowledgeBaseId().equals(knowledgeBaseId) && chunk.documentId().equals(documentId));
    }

    @Override
    public KnowledgeChunkVector saveChunkVector(KnowledgeChunkVector vector) {
        chunkVectors.removeIf(current -> current.chunkId().equals(vector.chunkId()));
        chunkVectors.add(vector);
        return vector;
    }

    @Override
    public List<KnowledgeChunkVector> listChunkVectors(String knowledgeBaseId) {
        return chunkVectors.stream()
                .filter(vector -> vector.knowledgeBaseId().equals(knowledgeBaseId))
                .toList();
    }

    @Override
    public void deleteChunkVectors(String knowledgeBaseId) {
        chunkVectors.removeIf(vector -> vector.knowledgeBaseId().equals(knowledgeBaseId));
    }

    @Override
    public void deleteChunkVectors(String knowledgeBaseId, String documentId) {
        chunkVectors.removeIf(vector -> vector.knowledgeBaseId().equals(knowledgeBaseId) && vector.documentId().equals(documentId));
    }

    private void deleteKnowledgeBaseOnly(String id) {
        knowledgeBases.removeIf(knowledgeBase -> knowledgeBase.id().equals(id));
    }
}
