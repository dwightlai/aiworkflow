package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeChunkPreview;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import com.aiworkflow.knowledge.domain.KnowledgeSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeBaseService {
    private final KnowledgeSplitter splitter;
    private final KnowledgeStore store;

    public KnowledgeBaseService() {
        this(new KnowledgeSplitter(), new InMemoryKnowledgeStore());
    }

    @Autowired
    public KnowledgeBaseService(KnowledgeSplitter splitter, KnowledgeStore store) {
        this.splitter = splitter;
        this.store = store;
    }

    public KnowledgeBase create(
            String name,
            String description,
            String embeddingModelId,
            String vectorStoreConfigId,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String retrievalMode,
            int topK
    ) {
        Instant now = Instant.now();
        KnowledgeBase knowledgeBase = new KnowledgeBase(
                "kb_" + UUID.randomUUID(),
                name,
                description,
                blankToNull(embeddingModelId),
                blankToNull(vectorStoreConfigId),
                defaultString(splitterType, "SIMPLE_TEXT"),
                chunkSize <= 0 ? 500 : chunkSize,
                Math.max(0, chunkOverlap),
                defaultString(retrievalMode, "KEYWORD"),
                topK <= 0 ? 3 : topK,
                "READY",
                0,
                0,
                now,
                now
        );
        return store.saveKnowledgeBase(knowledgeBase);
    }

    public KnowledgeBase create(String name, String description) {
        return create(name, description, null, null, "SIMPLE_TEXT", 500, 0, "KEYWORD", 3);
    }

    public List<KnowledgeBase> list() {
        return store.listKnowledgeBases();
    }

    public KnowledgeBase update(
            String id,
            String name,
            String description,
            String embeddingModelId,
            String vectorStoreConfigId,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String retrievalMode,
            int topK
    ) {
        KnowledgeBase current = getKnowledgeBase(id);
        KnowledgeBase updated = new KnowledgeBase(
                current.id(),
                defaultString(name, current.name()),
                description,
                blankToNull(embeddingModelId),
                blankToNull(vectorStoreConfigId),
                defaultString(splitterType, current.splitterType()),
                chunkSize <= 0 ? current.chunkSize() : chunkSize,
                Math.max(0, chunkOverlap),
                defaultString(retrievalMode, current.retrievalMode()),
                topK <= 0 ? current.topK() : topK,
                current.status(),
                current.documentCount(),
                current.chunkCount(),
                current.createdAt(),
                Instant.now()
        );
        return store.saveKnowledgeBase(updated);
    }

    public void delete(String id) {
        ensureKnowledgeBaseExists(id);
        store.deleteKnowledgeBase(id);
    }

    public List<KnowledgeChunkPreview> previewChunks(String content, String splitterType, int chunkSize, int chunkOverlap) {
        return splitter.preview(content, splitterType, chunkSize, chunkOverlap);
    }

    public KnowledgeDocument addDocument(
            String knowledgeBaseId,
            String name,
            String content,
            String splitterType,
            int chunkSize,
            int chunkOverlap
    ) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        String effectiveSplitterType = defaultString(splitterType, knowledgeBase.splitterType());
        int effectiveChunkSize = chunkSize <= 0 ? knowledgeBase.chunkSize() : chunkSize;
        int effectiveChunkOverlap = chunkOverlap < 0 ? knowledgeBase.chunkOverlap() : chunkOverlap;
        List<KnowledgeChunkPreview> previews = splitter.preview(content, effectiveSplitterType, effectiveChunkSize, effectiveChunkOverlap);
        KnowledgeDocument document = new KnowledgeDocument(
                "doc_" + UUID.randomUUID(),
                knowledgeBaseId,
                name,
                previews.size(),
                Instant.now()
        );
        store.saveDocument(document);
        for (KnowledgeChunkPreview preview : previews) {
            store.saveChunk(new KnowledgeChunk(
                    "chunk_" + UUID.randomUUID(),
                    knowledgeBaseId,
                    document.id(),
                    document.name(),
                    preview.content(),
                    preview.index(),
                    true,
                    preview.tokenEstimate()
            ));
        }
        refreshKnowledgeBaseStats(knowledgeBaseId);
        return document;
    }

    public KnowledgeDocument addDocument(String knowledgeBaseId, String name, String content) {
        return addDocument(knowledgeBaseId, name, content, null, 0, -1);
    }

    public List<KnowledgeDocument> listDocuments(String knowledgeBaseId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        return store.listDocuments(knowledgeBaseId);
    }

    public List<KnowledgeChunk> listChunks(String knowledgeBaseId, String documentId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        return store.listChunks(knowledgeBaseId, documentId);
    }

    public KnowledgeChunk updateChunk(String knowledgeBaseId, String chunkId, String content, boolean enabled) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        for (KnowledgeChunk current : store.listChunks(knowledgeBaseId)) {
            if (current.id().equals(chunkId)) {
                String updatedContent = content == null || content.isBlank() ? current.content() : content;
                return store.saveChunk(new KnowledgeChunk(
                        current.id(),
                        current.knowledgeBaseId(),
                        current.documentId(),
                        current.documentName(),
                        updatedContent,
                        current.index(),
                        enabled,
                        splitter.estimateTokens(updatedContent)
                ));
            }
        }
        throw new IllegalArgumentException("Knowledge chunk not found: " + chunkId);
    }

    public void deleteDocument(String knowledgeBaseId, String documentId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        store.deleteChunks(knowledgeBaseId, documentId);
        store.deleteDocument(knowledgeBaseId, documentId);
        refreshKnowledgeBaseStats(knowledgeBaseId);
    }

    public List<KnowledgeSearchResult> search(String knowledgeBaseId, String query, int topK) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        Set<String> terms = tokenize(query);
        int limit = topK <= 0 ? knowledgeBase.topK() : topK;
        return store.listChunks(knowledgeBaseId).stream()
                .filter(KnowledgeChunk::enabled)
                .map(chunk -> new KnowledgeSearchResult(
                        chunk.id(),
                        chunk.documentName(),
                        chunk.content(),
                        score(chunk.content(), terms)
                ))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(KnowledgeSearchResult::score).reversed())
                .limit(limit)
                .toList();
    }

    private KnowledgeBase getKnowledgeBase(String knowledgeBaseId) {
        return store.findKnowledgeBaseById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private void ensureKnowledgeBaseExists(String knowledgeBaseId) {
        getKnowledgeBase(knowledgeBaseId);
    }

    private void refreshKnowledgeBaseStats(String knowledgeBaseId) {
        KnowledgeBase current = getKnowledgeBase(knowledgeBaseId);
        int documentCount = store.listDocuments(knowledgeBaseId).size();
        int chunkCount = store.listChunks(knowledgeBaseId).size();
        store.saveKnowledgeBase(new KnowledgeBase(
                current.id(),
                current.name(),
                current.description(),
                current.embeddingModelId(),
                current.vectorStoreConfigId(),
                current.splitterType(),
                current.chunkSize(),
                current.chunkOverlap(),
                current.retrievalMode(),
                current.topK(),
                current.status(),
                documentCount,
                chunkCount,
                current.createdAt(),
                Instant.now()
        ));
    }

    private Set<String> tokenize(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        Set<String> terms = new LinkedHashSet<>();
        for (String term : normalized.split("[\\s,，。；;:：]+")) {
            if (!term.isBlank()) {
                terms.add(term);
            }
        }
        normalized.codePoints()
                .filter(Character::isLetterOrDigit)
                .mapToObj(Character::toString)
                .forEach(terms::add);
        return terms;
    }

    private int score(String content, Set<String> terms) {
        String normalized = content.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (!term.isBlank() && normalized.contains(term)) {
                score += term.length() > 1 ? 2 : 1;
            }
        }
        return score;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
