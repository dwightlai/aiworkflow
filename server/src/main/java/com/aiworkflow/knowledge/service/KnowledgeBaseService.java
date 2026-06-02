package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeChunkPreview;
import com.aiworkflow.knowledge.domain.KnowledgeChunkVector;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import com.aiworkflow.knowledge.domain.KnowledgeSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeBaseService {
    private static final int DEFAULT_VECTOR_DIMENSION = 1536;
    private final KnowledgeSplitter splitter;
    private final KnowledgeStore store;
    private final EmbeddingClient embeddingClient;
    private final DocumentTextExtractor documentTextExtractor;

    public KnowledgeBaseService() {
        this(new KnowledgeSplitter(), new InMemoryKnowledgeStore(), new LocalEmbeddingClient(), new DocumentTextExtractor());
    }

    public KnowledgeBaseService(KnowledgeSplitter splitter, KnowledgeStore store) {
        this(splitter, store, new LocalEmbeddingClient(), new DocumentTextExtractor());
    }

    public KnowledgeBaseService(KnowledgeSplitter splitter, KnowledgeStore store, EmbeddingClient embeddingClient) {
        this(splitter, store, embeddingClient, new DocumentTextExtractor());
    }

    @Autowired
    public KnowledgeBaseService(
            KnowledgeSplitter splitter,
            KnowledgeStore store,
            EmbeddingClient embeddingClient,
            DocumentTextExtractor documentTextExtractor
    ) {
        this.splitter = splitter;
        this.store = store;
        this.embeddingClient = embeddingClient;
        this.documentTextExtractor = documentTextExtractor;
    }

    public KnowledgeBase create(
            String name,
            String description,
            String embeddingModelId,
            String vectorStoreConfigId,
            int vectorDimension,
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
                normalizeVectorDimension(vectorDimension),
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
        return create(name, description, null, null, DEFAULT_VECTOR_DIMENSION, "SIMPLE_TEXT", 500, 0, "KEYWORD", 3);
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
        return create(
                name,
                description,
                embeddingModelId,
                vectorStoreConfigId,
                DEFAULT_VECTOR_DIMENSION,
                splitterType,
                chunkSize,
                chunkOverlap,
                retrievalMode,
                topK
        );
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
            int vectorDimension,
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
                vectorDimension <= 0 ? current.vectorDimension() : vectorDimension,
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

    public UploadedDocumentPreview previewDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            String splitterType,
            int chunkSize,
            int chunkOverlap
    ) {
        String content = documentTextExtractor.extract(fileName, contentType, inputStream);
        List<KnowledgeChunkPreview> chunks = previewChunks(content, splitterType, chunkSize, chunkOverlap);
        return new UploadedDocumentPreview(fileName, content.length(), chunks);
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
            KnowledgeChunk chunk = store.saveChunk(new KnowledgeChunk(
                    "chunk_" + UUID.randomUUID(),
                    knowledgeBaseId,
                    document.id(),
                    document.name(),
                    preview.content(),
                    preview.index(),
                    true,
                    preview.tokenEstimate()
            ));
            saveEmbeddingIfConfigured(knowledgeBase, chunk);
        }
        refreshKnowledgeBaseStats(knowledgeBaseId);
        return document;
    }

    public KnowledgeDocument addDocument(String knowledgeBaseId, String name, String content) {
        return addDocument(knowledgeBaseId, name, content, null, 0, -1);
    }

    public KnowledgeDocument addDocumentFile(
            String knowledgeBaseId,
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            String splitterType,
            int chunkSize,
            int chunkOverlap
    ) {
        String content = documentTextExtractor.extract(fileName, contentType, inputStream);
        return addDocument(knowledgeBaseId, fileName, content, splitterType, chunkSize, chunkOverlap);
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
                KnowledgeChunk updated = store.saveChunk(new KnowledgeChunk(
                        current.id(),
                        current.knowledgeBaseId(),
                        current.documentId(),
                        current.documentName(),
                        updatedContent,
                        current.index(),
                        enabled,
                        splitter.estimateTokens(updatedContent)
                ));
                saveEmbeddingIfConfigured(getKnowledgeBase(knowledgeBaseId), updated);
                return updated;
            }
        }
        throw new IllegalArgumentException("Knowledge chunk not found: " + chunkId);
    }

    public void deleteDocument(String knowledgeBaseId, String documentId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        store.deleteChunkVectors(knowledgeBaseId, documentId);
        store.deleteChunks(knowledgeBaseId, documentId);
        store.deleteDocument(knowledgeBaseId, documentId);
        refreshKnowledgeBaseStats(knowledgeBaseId);
    }

    public List<KnowledgeSearchResult> search(String knowledgeBaseId, String query, int topK) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        Set<String> terms = tokenize(query);
        int limit = topK <= 0 ? knowledgeBase.topK() : topK;
        Map<String, KnowledgeChunkVector> vectorsByChunkId = vectorsByChunkId(knowledgeBaseId);
        List<Double> queryEmbedding = shouldUseVector(knowledgeBase)
                ? embeddingClient.embed(knowledgeBase.embeddingModelId(), knowledgeBase.embeddingModelId(), query)
                : List.of();
        return store.listChunks(knowledgeBaseId).stream()
                .filter(KnowledgeChunk::enabled)
                .map(chunk -> new KnowledgeSearchResult(
                        chunk.id(),
                        chunk.documentName(),
                        chunk.content(),
                        combinedScore(chunk, terms, vectorsByChunkId.get(chunk.id()), queryEmbedding, knowledgeBase.retrievalMode())
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
                current.vectorDimension(),
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

    private void saveEmbeddingIfConfigured(KnowledgeBase knowledgeBase, KnowledgeChunk chunk) {
        if (!shouldUseVector(knowledgeBase)) {
            return;
        }
        store.saveChunkVector(new KnowledgeChunkVector(
                chunk.id(),
                chunk.knowledgeBaseId(),
                chunk.documentId(),
                knowledgeBase.embeddingModelId(),
                embeddingClient.embed(knowledgeBase.embeddingModelId(), knowledgeBase.embeddingModelId(), chunk.content()),
                Instant.now()
        ));
    }

    private boolean shouldUseVector(KnowledgeBase knowledgeBase) {
        return knowledgeBase.embeddingModelId() != null && !knowledgeBase.embeddingModelId().isBlank()
                && ("VECTOR".equalsIgnoreCase(knowledgeBase.retrievalMode()) || "HYBRID".equalsIgnoreCase(knowledgeBase.retrievalMode()));
    }

    private Map<String, KnowledgeChunkVector> vectorsByChunkId(String knowledgeBaseId) {
        Map<String, KnowledgeChunkVector> vectors = new HashMap<>();
        for (KnowledgeChunkVector vector : store.listChunkVectors(knowledgeBaseId)) {
            vectors.put(vector.chunkId(), vector);
        }
        return vectors;
    }

    private int combinedScore(
            KnowledgeChunk chunk,
            Set<String> terms,
            KnowledgeChunkVector vector,
            List<Double> queryEmbedding,
            String retrievalMode
    ) {
        int keywordScore = score(chunk.content(), terms);
        int vectorScore = vector == null || queryEmbedding.isEmpty() ? 0 : (int) Math.round(cosine(queryEmbedding, vector.embedding()) * 1000);
        if ("VECTOR".equalsIgnoreCase(retrievalMode)) {
            return vectorScore;
        }
        if ("HYBRID".equalsIgnoreCase(retrievalMode)) {
            return keywordScore * 100 + vectorScore;
        }
        return keywordScore;
    }

    private double cosine(List<Double> left, List<Double> right) {
        int size = Math.min(left.size(), right.size());
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int index = 0; index < size; index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
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

    private int normalizeVectorDimension(int vectorDimension) {
        return vectorDimension <= 0 ? DEFAULT_VECTOR_DIMENSION : vectorDimension;
    }

    public record UploadedDocumentPreview(
            String fileName,
            int characterCount,
            List<KnowledgeChunkPreview> chunks
    ) {
    }
}
