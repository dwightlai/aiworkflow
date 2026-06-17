package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.config.AgiStorageSettingsService;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.KnowledgeDocument;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.mw.ai.agi.asset.service.AssetGrantService;
import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {
    private static final int DEFAULT_VECTOR_DIMENSION = 1536;
    private static final int DEFAULT_MIN_RELEVANCE_SCORE = 350;
    private static final int EMBEDDING_BACKFILL_BATCH_SIZE = 16;
    private static final String DEFAULT_SPLITTER_TYPE = "SIMPLE_TEXT";
    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;
    private static final String DEFAULT_RETRIEVAL_MODE = "HYBRID";
    private static final int DEFAULT_TOP_K = 3;
    private final KnowledgeSplitter splitter;
    private final KnowledgeStore store;
    private final EmbeddingClient embeddingClient;
    private final DocumentTextExtractor documentTextExtractor;
    private final KnowledgeDocumentSplitter documentSplitter;
    private final TableDocumentParser tableDocumentParser;
    private final VectorStoreConfigStore vectorStoreConfigStore;
    private final ElasticsearchVectorStoreClient elasticsearchVectorStoreClient;
    private final AssetGrantService assetGrantService;
    private final TenantBusinessGuard tenantGuard;
    private final KnowledgeDocumentFileStorage knowledgeDocumentFileStorage;
    private KnowledgeDatasetService datasetService;
    private com.mw.ai.agi.knowledge.persistence.KnowledgeSourceIndexMapper sourceIndexMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeBaseService() {
        this(new KnowledgeSplitter(), new InMemoryKnowledgeStore(), new LocalEmbeddingClient());
    }

    public KnowledgeBaseService(KnowledgeSplitter splitter, KnowledgeStore store) {
        this(splitter, store, new LocalEmbeddingClient());
    }

    public KnowledgeBaseService(KnowledgeSplitter splitter, KnowledgeStore store, EmbeddingClient embeddingClient) {
        this(
                splitter,
                store,
                embeddingClient,
                new DocumentTextExtractor(),
                new KnowledgeDocumentSplitter(splitter),
                new TableDocumentParser(),
                new InMemoryVectorStoreConfigStore(),
                new ElasticsearchVectorStoreClient(new ObjectMapper()),
                null,
                null,
                new KnowledgeDocumentFileStorage(AgiStorageSettingsService.withDefaults(
                        new com.mw.ai.agi.config.AgiStorageProperties(),
                        new ObjectMapper()
                ))
        );
    }

    public KnowledgeBaseService(
            KnowledgeSplitter splitter,
            KnowledgeStore store,
            EmbeddingClient embeddingClient,
            DocumentTextExtractor documentTextExtractor
    ) {
        this(
                splitter,
                store,
                embeddingClient,
                documentTextExtractor,
                new KnowledgeDocumentSplitter(splitter),
                new TableDocumentParser(),
                new InMemoryVectorStoreConfigStore(),
                new ElasticsearchVectorStoreClient(new ObjectMapper()),
                null,
                null,
                new KnowledgeDocumentFileStorage(AgiStorageSettingsService.withDefaults(
                        new com.mw.ai.agi.config.AgiStorageProperties(),
                        new ObjectMapper()
                ))
        );
    }

    @Autowired
    public KnowledgeBaseService(
            KnowledgeSplitter splitter,
            KnowledgeStore store,
            EmbeddingClient embeddingClient,
            DocumentTextExtractor documentTextExtractor,
            KnowledgeDocumentSplitter documentSplitter,
            TableDocumentParser tableDocumentParser,
            VectorStoreConfigStore vectorStoreConfigStore,
            ElasticsearchVectorStoreClient elasticsearchVectorStoreClient,
            AssetGrantService assetGrantService,
            TenantBusinessGuard tenantGuard,
            KnowledgeDocumentFileStorage knowledgeDocumentFileStorage
    ) {
        this.splitter = splitter;
        this.store = store;
        this.embeddingClient = embeddingClient;
        this.documentTextExtractor = documentTextExtractor;
        this.documentSplitter = documentSplitter;
        this.tableDocumentParser = tableDocumentParser;
        this.vectorStoreConfigStore = vectorStoreConfigStore;
        this.elasticsearchVectorStoreClient = elasticsearchVectorStoreClient;
        this.assetGrantService = assetGrantService;
        this.tenantGuard = tenantGuard;
        this.knowledgeDocumentFileStorage = knowledgeDocumentFileStorage;
    }

    @Autowired(required = false)
    public void setDatasetService(KnowledgeDatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @Autowired(required = false)
    public void setSourceIndexMapper(com.mw.ai.agi.knowledge.persistence.KnowledgeSourceIndexMapper sourceIndexMapper) {
        this.sourceIndexMapper = sourceIndexMapper;
    }

    public KnowledgeBase create(
            String name,
            String description,
            String ownerUnitId,
            String embeddingModelId,
            String vectorStoreConfigId,
            int vectorDimension,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String retrievalMode,
            int topK,
            String kbType,
            String datasetMode
    ) {
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        String effectiveKbType = defaultString(kbType, "NORMAL");
        String effectiveDatasetMode = "MULTI";
        KnowledgeBase knowledgeBase = new KnowledgeBase(
                "kb_" + UUID.randomUUID(),
                currentTenantId(),
                name,
                description,
                blankToNull(ownerUnitId),
                blankToNull(embeddingModelId),
                blankToNull(vectorStoreConfigId),
                normalizeVectorDimension(vectorDimension),
                defaultString(splitterType, DEFAULT_SPLITTER_TYPE),
                chunkSize <= 0 ? DEFAULT_CHUNK_SIZE : chunkSize,
                chunkOverlap < 0 ? DEFAULT_CHUNK_OVERLAP : chunkOverlap,
                defaultString(retrievalMode, DEFAULT_RETRIEVAL_MODE),
                topK <= 0 ? DEFAULT_TOP_K : topK,
                "READY",
                0,
                0,
                operator,
                operator,
                now,
                now,
                effectiveKbType,
                null,
                effectiveDatasetMode,
                null,
                0,
                null
        );
        KnowledgeBase saved = store.saveKnowledgeBase(knowledgeBase);
        grantOwner(saved);
        if (datasetService != null) {
            com.mw.ai.agi.knowledge.domain.KnowledgeDataset dataset = datasetService.createDefaultDataset(saved);
            saved = store.saveKnowledgeBase(new KnowledgeBase(
                    saved.id(), saved.tenantId(), saved.name(), saved.description(), saved.ownerUnitId(),
                    saved.embeddingModelId(), saved.vectorStoreConfigId(), saved.vectorDimension(), saved.splitterType(),
                    saved.chunkSize(), saved.chunkOverlap(), saved.retrievalMode(), saved.topK(), saved.status(),
                    saved.documentCount(), saved.chunkCount(), saved.createdBy(), operator, saved.createdAt(), now,
                    saved.kbType(), saved.bizScope(), effectiveDatasetMode, dataset.id(), 1, saved.metadataJson()
            ));
        }
        return saved;
    }

    public KnowledgeBase create(
            String name,
            String description,
            String ownerUnitId,
            String embeddingModelId,
            String vectorStoreConfigId,
            int vectorDimension,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String retrievalMode,
            int topK
    ) {
        return create(name, description, ownerUnitId, embeddingModelId, vectorStoreConfigId, vectorDimension,
                splitterType, chunkSize, chunkOverlap, retrievalMode, topK, "NORMAL", "MULTI");
    }

    public KnowledgeBase create(String name, String description) {
        return create(
                name,
                description,
                null,
                null,
                null,
                DEFAULT_VECTOR_DIMENSION,
                DEFAULT_SPLITTER_TYPE,
                DEFAULT_CHUNK_SIZE,
                DEFAULT_CHUNK_OVERLAP,
                DEFAULT_RETRIEVAL_MODE,
                DEFAULT_TOP_K
        );
    }

    public KnowledgeBase create(
            String name,
            String description,
            String ownerUnitId,
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
                ownerUnitId,
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

    public List<KnowledgeBase> list(Map<String, Object> context) {
        return store.listKnowledgeBases(listTenantId());
    }

    public List<KnowledgeBase> list() {
        return list(Map.of());
    }

    public KnowledgeBase update(
            String id,
            String name,
            String description,
            String ownerUnitId,
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
        String operator = OperatorContext.currentUserId();
        KnowledgeBase updated = new KnowledgeBase(
                current.id(),
                current.tenantId(),
                defaultString(name, current.name()),
                description,
                blankToNull(ownerUnitId != null && !ownerUnitId.isBlank() ? ownerUnitId : current.ownerUnitId()),
                blankToNull(embeddingModelId),
                blankToNull(vectorStoreConfigId),
                vectorDimension <= 0 ? current.vectorDimension() : vectorDimension,
                defaultString(splitterType, current.splitterType()),
                chunkSize <= 0 ? current.chunkSize() : chunkSize,
                chunkOverlap < 0 ? current.chunkOverlap() : chunkOverlap,
                defaultString(retrievalMode, current.retrievalMode()),
                topK <= 0 ? current.topK() : topK,
                current.status(),
                current.documentCount(),
                current.chunkCount(),
                current.createdBy(),
                operator,
                current.createdAt(),
                Instant.now(),
                current.kbType(),
                current.bizScope(),
                current.datasetMode(),
                current.defaultDatasetId(),
                current.datasetCount(),
                current.metadataJson()
        );
        KnowledgeBase saved = store.saveKnowledgeBase(updated);
        grantOwner(saved);
        backfillEmbeddingsIfConfigured(saved);
        return saved;
    }

    private void assertKnowledgeBaseUseAllowed(KnowledgeBase knowledgeBase, Map<String, Object> context) {
        if (assetGrantService == null) {
            return;
        }
        if (!assetGrantService.isAllowed(
                AssetGrantService.KNOWLEDGE_BASE,
                knowledgeBase.id(),
                knowledgeBase.ownerUnitId(),
                context
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "知识库使用无权限：" + knowledgeBase.name());
        }
    }

    private void grantOwner(KnowledgeBase knowledgeBase) {
        if (assetGrantService == null || knowledgeBase.ownerUnitId() == null || knowledgeBase.ownerUnitId().isBlank()) {
            return;
        }
        assetGrantService.save(AssetGrantService.KNOWLEDGE_BASE, knowledgeBase.id(), AssetGrantService.USE, knowledgeBase.ownerUnitId(), AssetGrantService.SELF, null, AssetGrantService.SELF, true, null);
    }

    public void delete(String id) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(id);
        externalVectorStore(knowledgeBase)
                .ifPresent(config -> elasticsearchVectorStoreClient.deleteKnowledgeBase(config, id));
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

    public UploadedDocumentPreview previewTextDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest
    ) {
        String content = documentTextExtractor.extract(fileName, contentType, inputStream);
        List<KnowledgeChunkPreview> chunks = documentSplitter.splitText(content, splitRequest);
        return new UploadedDocumentPreview(fileName, content.length(), chunks);
    }

    public UploadedDocumentPreview previewTableDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest
    ) {
        String content = documentTextExtractor.extract(fileName, contentType, inputStream);
        List<TableDocumentParser.TableRow> rows = tableDocumentParser.parseText(content, sheetName(fileName));
        List<KnowledgeChunkPreview> chunks = documentSplitter.splitTableRows(rows, splitRequest);
        return new UploadedDocumentPreview(fileName, content.length(), chunks);
    }

    public KnowledgeDocument addDocument(
            String knowledgeBaseId,
            String name,
            String content,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String datasetId
    ) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        String effectiveSplitterType = defaultString(splitterType, knowledgeBase.splitterType());
        int effectiveChunkSize = chunkSize <= 0 ? knowledgeBase.chunkSize() : chunkSize;
        int effectiveChunkOverlap = chunkOverlap < 0 ? knowledgeBase.chunkOverlap() : chunkOverlap;
        List<KnowledgeChunkPreview> previews = splitter.preview(content, effectiveSplitterType, effectiveChunkSize, effectiveChunkOverlap);
        String effectiveDatasetId = resolveDatasetId(knowledgeBase, datasetId);
        KnowledgeDocument document = new KnowledgeDocument(
                "doc_" + UUID.randomUUID(),
                knowledgeBaseId,
                name,
                previews.size(),
                Instant.now()
        ).withDatasetId(effectiveDatasetId);
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
            ).withDatasetContext(
                    document.datasetId(), null, null, null, null, null, null
            ));
            saveEmbeddingIfConfigured(knowledgeBase, chunk);
        }
        refreshKnowledgeBaseStats(knowledgeBaseId);
        return document;
    }

    public KnowledgeDocument addDocument(
            String knowledgeBaseId,
            String name,
            String content,
            String splitterType,
            int chunkSize,
            int chunkOverlap
    ) {
        return addDocument(knowledgeBaseId, name, content, splitterType, chunkSize, chunkOverlap, null);
    }

    public KnowledgeDocument addDocument(String knowledgeBaseId, String name, String content) {
        return addDocument(knowledgeBaseId, name, content, null, 0, -1, null);
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

    public KnowledgeDocument addManualDataset(String knowledgeBaseId, List<ManualDatasetEntry> entries) {
        return addManualDataset(knowledgeBaseId, entries, null);
    }

    public KnowledgeDocument addManualDataset(String knowledgeBaseId, List<ManualDatasetEntry> entries, String datasetId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Manual dataset entries are required");
        }
        List<KnowledgeChunkPreview> previews = new java.util.ArrayList<>();
        for (ManualDatasetEntry entry : entries) {
            String content = manualEntryText(entry);
            if (content.isBlank()) {
                continue;
            }
            previews.add(new KnowledgeChunkPreview(previews.size(), content, estimateManualTokens(content)));
        }
        if (previews.isEmpty()) {
            throw new IllegalArgumentException("Manual dataset entries have no content");
        }
        KnowledgeSplitRequest splitRequest = new KnowledgeSplitRequest("MANUAL_ENTRY", DEFAULT_CHUNK_SIZE, null);
        KnowledgeDocument document = saveDocumentFromPreviews(
                knowledgeBaseId,
                "doc_" + UUID.randomUUID(),
                manualDatasetName(entries),
                previews,
                "MANUAL",
                combinedManualValue(entries, ManualDatasetEntry::tags),
                combinedManualValue(entries, ManualDatasetEntry::category),
                combinedManualValue(entries, ManualDatasetEntry::source),
                previews.size(),
                "MANUAL",
                splitRequest,
                previews.stream().map(KnowledgeChunkPreview::content).collect(java.util.stream.Collectors.joining("\n\n")),
                null,
                datasetId
        );
        refreshKnowledgeBaseStats(knowledgeBaseId);
        return document;
    }

    public KnowledgeDocument addTextDocumentFile(
            String knowledgeBaseId,
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest
    ) {
        return addTextDocumentFile(knowledgeBaseId, fileName, contentType, inputStream, splitRequest, null);
    }

    public KnowledgeDocument addTextDocumentFile(
            String knowledgeBaseId,
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest,
            String datasetId
    ) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            String documentId = "doc_" + UUID.randomUUID();
            String storagePath = knowledgeDocumentFileStorage
                    .save(knowledgeBaseId, documentId, fileName, bytes)
                    .orElse(null);
            String content = documentTextExtractor.extract(
                    fileName,
                    contentType,
                    new java.io.ByteArrayInputStream(bytes)
            );
            KnowledgeDocument document = saveDocumentFromPreviews(
                    knowledgeBaseId,
                    documentId,
                    fileName,
                    documentSplitter.splitText(content, splitRequest),
                    "TEXT_DOCUMENT",
                    null,
                    null,
                    fileName,
                    0,
                    "TEXT",
                    splitRequest,
                    content,
                    storagePath,
                    datasetId
            );
            refreshKnowledgeBaseStats(knowledgeBaseId);
            return document;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read uploaded knowledge document.", exception);
        }
    }

    public KnowledgeDocument addTableDocumentFile(
            String knowledgeBaseId,
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest
    ) {
        return addTableDocumentFile(knowledgeBaseId, fileName, contentType, inputStream, splitRequest, null);
    }

    public KnowledgeDocument addTableDocumentFile(
            String knowledgeBaseId,
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest,
            String datasetId
    ) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            String documentId = "doc_" + UUID.randomUUID();
            String storagePath = knowledgeDocumentFileStorage
                    .save(knowledgeBaseId, documentId, fileName, bytes)
                    .orElse(null);
            String content = documentTextExtractor.extract(
                    fileName,
                    contentType,
                    new java.io.ByteArrayInputStream(bytes)
            );
            List<TableDocumentParser.TableRow> rows = tableDocumentParser.parseText(content, sheetName(fileName));
            KnowledgeDocument document = saveDocumentFromPreviews(
                    knowledgeBaseId,
                    documentId,
                    fileName,
                    documentSplitter.splitTableRows(rows, splitRequest),
                    "TABLE_DOCUMENT",
                    null,
                    null,
                    fileName,
                    rows.size(),
                    "TABLE",
                    splitRequest,
                    content,
                    storagePath,
                    datasetId
            );
            refreshKnowledgeBaseStats(knowledgeBaseId);
            return document;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read uploaded knowledge document.", exception);
        }
    }

    public List<KnowledgeDocument> listDocuments(String knowledgeBaseId) {
        return listDocuments(knowledgeBaseId, null);
    }

    public List<KnowledgeDocument> listDocuments(String knowledgeBaseId, String datasetId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        List<KnowledgeDocument> documents = store.listDocuments(knowledgeBaseId);
        if (datasetId == null || datasetId.isBlank()) {
            return documents;
        }
        String defaultDatasetId = resolveDefaultDatasetId(knowledgeBase);
        return documents.stream()
                .filter(document -> matchesDocumentDataset(document, datasetId, defaultDatasetId))
                .toList();
    }

    public KnowledgeBase ensureDefaultDataset(KnowledgeBase knowledgeBase) {
        if (datasetService == null) {
            return knowledgeBase;
        }
        boolean hasDefault = datasetService.listByKnowledgeBase(knowledgeBase.id()).stream()
                .anyMatch(dataset -> "DEFAULT".equalsIgnoreCase(dataset.datasetType()));
        if (hasDefault) {
            if (knowledgeBase.defaultDatasetId() == null || knowledgeBase.defaultDatasetId().isBlank()) {
                com.mw.ai.agi.knowledge.domain.KnowledgeDataset defaultDataset = datasetService.listByKnowledgeBase(knowledgeBase.id()).stream()
                        .filter(dataset -> "DEFAULT".equalsIgnoreCase(dataset.datasetType()))
                        .findFirst()
                        .orElse(null);
                if (defaultDataset != null) {
                    return updateDefaultDatasetBinding(knowledgeBase, defaultDataset.id());
                }
            }
            return knowledgeBase;
        }
        com.mw.ai.agi.knowledge.domain.KnowledgeDataset dataset = datasetService.createDefaultDataset(knowledgeBase);
        return updateDefaultDatasetBinding(knowledgeBase, dataset.id());
    }

    private KnowledgeBase updateDefaultDatasetBinding(KnowledgeBase knowledgeBase, String defaultDatasetId) {
        int datasetCount = datasetService.listByKnowledgeBase(knowledgeBase.id()).size();
        return store.saveKnowledgeBase(new KnowledgeBase(
                knowledgeBase.id(), knowledgeBase.tenantId(), knowledgeBase.name(), knowledgeBase.description(),
                knowledgeBase.ownerUnitId(), knowledgeBase.embeddingModelId(), knowledgeBase.vectorStoreConfigId(),
                knowledgeBase.vectorDimension(), knowledgeBase.splitterType(), knowledgeBase.chunkSize(),
                knowledgeBase.chunkOverlap(), knowledgeBase.retrievalMode(), knowledgeBase.topK(), knowledgeBase.status(),
                knowledgeBase.documentCount(), knowledgeBase.chunkCount(), knowledgeBase.createdBy(),
                OperatorContext.currentUserId(), knowledgeBase.createdAt(), Instant.now(), knowledgeBase.kbType(),
                knowledgeBase.bizScope(), "MULTI", defaultDatasetId, datasetCount, knowledgeBase.metadataJson()
        ));
    }

    private boolean matchesDocumentDataset(KnowledgeDocument document, String datasetId, String defaultDatasetId) {
        String effectiveDatasetId = document.datasetId() == null || document.datasetId().isBlank()
                ? defaultDatasetId
                : document.datasetId();
        return datasetId.equals(effectiveDatasetId);
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
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        externalVectorStore(knowledgeBase)
                .ifPresent(config -> elasticsearchVectorStoreClient.deleteDocument(config, knowledgeBaseId, documentId));
        store.deleteChunkVectors(knowledgeBaseId, documentId);
        store.deleteChunks(knowledgeBaseId, documentId);
        store.deleteDocument(knowledgeBaseId, documentId);
        refreshKnowledgeBaseStats(knowledgeBaseId);
    }

    public KnowledgeDocument reparseDocument(String knowledgeBaseId, String documentId) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        KnowledgeDocument current = store.listDocuments(knowledgeBaseId).stream()
                .filter(document -> document.id().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
        KnowledgeDocument processing = withStatus(current, "PROCESSING", null);
        store.saveDocument(processing);
        try {
            KnowledgeSplitRequest splitRequest = splitRequestFrom(current);
            List<KnowledgeChunkPreview> previews = "TABLE_DOCUMENT".equalsIgnoreCase(current.datasetType())
                    ? documentSplitter.splitTableRows(tableDocumentParser.parseText(current.rawContent(), sheetName(current.name())), splitRequest)
                    : documentSplitter.splitText(current.rawContent(), splitRequest);
            externalVectorStore(knowledgeBase)
                    .ifPresent(config -> elasticsearchVectorStoreClient.deleteDocument(config, knowledgeBaseId, documentId));
            store.deleteChunkVectors(knowledgeBaseId, documentId);
            store.deleteChunks(knowledgeBaseId, documentId);
            KnowledgeDocument reparsed = withChunkCount(withStatus(processing, "READY", null), previews.size());
            store.saveDocument(reparsed);
            saveChunksForDocument(knowledgeBase, reparsed, previews);
            refreshKnowledgeBaseStats(knowledgeBaseId);
            return reparsed;
        } catch (RuntimeException exception) {
            KnowledgeDocument failed = withStatus(processing, "FAILED", exception.getMessage());
            store.saveDocument(failed);
            throw exception;
        }
    }

    public List<KnowledgeSearchResult> search(String knowledgeBaseId, String query, int topK) {
        return search(knowledgeBaseId, query, topK, Map.of());
    }

    public List<KnowledgeSearchResult> search(String knowledgeBaseId, String query, int topK, Map<String, Object> context) {
        return search(knowledgeBaseId, null, query, topK, context);
    }

    public List<KnowledgeSearchResult> search(String knowledgeBaseId, String datasetId, String query, int topK, Map<String, Object> context) {
        return search(knowledgeBaseId, datasetId, query, topK, context, KnowledgeSearchOptions.empty());
    }

    public List<KnowledgeSearchResult> search(
            String knowledgeBaseId,
            String datasetId,
            String query,
            int topK,
            Map<String, Object> context,
            KnowledgeSearchOptions options
    ) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        assertKnowledgeBaseUseAllowed(knowledgeBase, context);
        KnowledgeSearchOptions effectiveOptions = options == null ? KnowledgeSearchOptions.empty() : options;
        String effectiveRetrievalMode = resolveRetrievalMode(knowledgeBase, effectiveOptions.retrievalMode());
        KnowledgeRetrievalFilters filters = effectiveOptions.filters() == null
                ? KnowledgeRetrievalFilters.empty()
                : effectiveOptions.filters();
        String effectiveDatasetId = resolveDatasetId(knowledgeBase, datasetId);
        Set<String> terms = tokenize(query);
        int limit = Math.min(topK <= 0 ? knowledgeBase.topK() : topK, 50);
        Optional<VectorStoreConfig> externalStore = shouldUseVector(knowledgeBase, effectiveRetrievalMode)
                ? externalVectorStore(knowledgeBase)
                : Optional.empty();
        if (externalStore.isPresent()) {
            List<Double> queryEmbedding = embeddingClient.embed(knowledgeBase.embeddingModelId(), knowledgeBase.embeddingModelId(), query);
            List<KnowledgeSearchResult> vectorResults = elasticsearchVectorStoreClient.search(
                            externalStore.get(),
                            knowledgeBaseId,
                            effectiveDatasetId,
                            queryEmbedding,
                            "HYBRID".equalsIgnoreCase(effectiveRetrievalMode) ? Math.max(limit * 3, limit) : limit
                    )
                    .stream()
                    .map(hit -> new KnowledgeSearchResult(
                            hit.chunkId(),
                            hit.documentName(),
                            hit.content(),
                            (int) Math.round(hit.score() * 1000)
                    ))
                    .filter(result -> result.score() > 0)
                    .toList();
            vectorResults = filterSearchResults(knowledgeBaseId, vectorResults, filters);
            if (!"HYBRID".equalsIgnoreCase(effectiveRetrievalMode)) {
                return applyRelevanceThreshold(vectorResults.stream().limit(limit).toList());
            }
            return applyRelevanceThreshold(mergeSearchResults(
                    vectorResults,
                    localKeywordResults(knowledgeBaseId, effectiveDatasetId, terms, filters),
                    limit
            ));
        }
        Map<String, KnowledgeChunkVector> vectorsByChunkId = vectorsByChunkId(knowledgeBaseId);
        List<Double> queryEmbedding = shouldUseVector(knowledgeBase, effectiveRetrievalMode)
                ? embeddingClient.embed(knowledgeBase.embeddingModelId(), knowledgeBase.embeddingModelId(), query)
                : List.of();
        List<KnowledgeSearchResult> results = store.listChunks(knowledgeBaseId).stream()
                .filter(KnowledgeChunk::enabled)
                .filter(chunk -> matchesDataset(chunk, effectiveDatasetId))
                .filter(filters::matches)
                .map(chunk -> new KnowledgeSearchResult(
                        chunk.id(),
                        chunk.documentName(),
                        chunk.content(),
                        combinedScore(chunk, terms, vectorsByChunkId.get(chunk.id()), queryEmbedding, effectiveRetrievalMode)
                ))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(KnowledgeSearchResult::score).reversed())
                .limit(limit)
                .toList();
        return applyRelevanceThreshold(results);
    }

    public List<KnowledgeSearchResult> searchMany(List<String> knowledgeBaseIds, String query, int topK) {
        return searchMany(knowledgeBaseIds, query, topK, Map.of());
    }

    public List<KnowledgeSearchResult> searchMany(List<String> knowledgeBaseIds, String query, int topK, Map<String, Object> context) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return List.of();
        }
        int limit = Math.max(topK <= 0 ? 5 : topK, 1);
        Map<String, KnowledgeSearchResult> merged = new HashMap<>();
        for (String knowledgeBaseId : knowledgeBaseIds) {
            if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
                continue;
            }
            for (KnowledgeSearchResult result : search(knowledgeBaseId, query, limit, context)) {
                KnowledgeSearchResult current = merged.get(result.id());
                merged.put(result.id(), current == null
                        ? result
                        : new KnowledgeSearchResult(
                                current.id(),
                                current.documentName(),
                                current.content(),
                                current.score() + result.score()
                        ));
            }
        }
        return applyRelevanceThreshold(merged.values().stream()
                .sorted(Comparator.comparingInt(KnowledgeSearchResult::score).reversed())
                .limit(limit)
                .toList());
    }

    public List<KnowledgeSearchResult> searchDocument(String knowledgeBaseId, String documentId, String query, int topK) {
        return searchDocument(knowledgeBaseId, documentId, query, topK, Map.of());
    }

    public List<KnowledgeSearchResult> searchDocument(String knowledgeBaseId, String documentId, String query, int topK, Map<String, Object> context) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        assertKnowledgeBaseUseAllowed(knowledgeBase, context);
        boolean documentExists = store.listDocuments(knowledgeBaseId).stream()
                .anyMatch(document -> document.id().equals(documentId));
        if (!documentExists) {
            throw new IllegalArgumentException("Knowledge document not found: " + documentId);
        }
        Set<String> terms = tokenize(query);
        int limit = Math.min(topK <= 0 ? knowledgeBase.topK() : topK, 50);
        Map<String, KnowledgeChunkVector> vectorsByChunkId = vectorsByChunkId(knowledgeBaseId);
        List<Double> queryEmbedding = shouldUseVector(knowledgeBase)
                ? embeddingClient.embed(knowledgeBase.embeddingModelId(), knowledgeBase.embeddingModelId(), query)
                : List.of();
        List<KnowledgeSearchResult> results = store.listChunks(knowledgeBaseId, documentId).stream()
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
        return applyRelevanceThreshold(results);
    }

    private List<KnowledgeSearchResult> localKeywordResults(
            String knowledgeBaseId,
            String datasetId,
            Set<String> terms,
            KnowledgeRetrievalFilters filters
    ) {
        return store.listChunks(knowledgeBaseId).stream()
                .filter(KnowledgeChunk::enabled)
                .filter(chunk -> matchesDataset(chunk, datasetId))
                .filter(filters::matches)
                .map(chunk -> new KnowledgeSearchResult(
                        chunk.id(),
                        chunk.documentName(),
                        chunk.content(),
                        score(chunk.content(), terms) * 1000
                ))
                .filter(result -> result.score() > 0)
                .toList();
    }

    private List<KnowledgeSearchResult> filterSearchResults(
            String knowledgeBaseId,
            List<KnowledgeSearchResult> results,
            KnowledgeRetrievalFilters filters
    ) {
        if (filters.isEmpty()) {
            return results;
        }
        Map<String, KnowledgeChunk> chunksById = store.listChunks(knowledgeBaseId).stream()
                .collect(Collectors.toMap(KnowledgeChunk::id, chunk -> chunk, (left, right) -> left));
        return results.stream()
                .filter(result -> filters.matches(chunksById.get(result.id())))
                .toList();
    }

    private String resolveRetrievalMode(KnowledgeBase knowledgeBase, String requestedMode) {
        if (requestedMode != null && !requestedMode.isBlank()) {
            String normalized = requestedMode.trim().toUpperCase(Locale.ROOT);
            if ("KEYWORD".equals(normalized) || "VECTOR".equals(normalized) || "HYBRID".equals(normalized)) {
                return normalized;
            }
            throw new IllegalArgumentException("Unsupported retrievalMode: " + requestedMode);
        }
        String configured = knowledgeBase.retrievalMode();
        return configured == null || configured.isBlank() ? DEFAULT_RETRIEVAL_MODE : configured.trim().toUpperCase(Locale.ROOT);
    }

    private List<KnowledgeSearchResult> mergeSearchResults(
            List<KnowledgeSearchResult> vectorResults,
            List<KnowledgeSearchResult> keywordResults,
            int limit
    ) {
        Map<String, KnowledgeSearchResult> merged = new HashMap<>();
        for (KnowledgeSearchResult result : vectorResults) {
            merged.put(result.id(), result);
        }
        for (KnowledgeSearchResult result : keywordResults) {
            KnowledgeSearchResult current = merged.get(result.id());
            merged.put(result.id(), current == null
                    ? result
                    : new KnowledgeSearchResult(
                            current.id(),
                            current.documentName(),
                            current.content(),
                            current.score() + result.score()
                    ));
        }
        return merged.values().stream()
                .sorted(Comparator.comparingInt(KnowledgeSearchResult::score).reversed())
                .limit(limit)
                .toList();
    }

    public KnowledgeBase getKnowledgeBase(String knowledgeBaseId) {
        KnowledgeBase knowledgeBase = store.findKnowledgeBaseById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
        assertTenantAccessible(knowledgeBase.tenantId());
        return knowledgeBase;
    }

    private String currentTenantId() {
        return tenantGuard == null ? TenantContext.requireTenantId() : tenantGuard.currentTenantId();
    }

    private String listTenantId() {
        return currentTenantId();
    }

    private void assertTenantAccessible(String resourceTenantId) {
        if (tenantGuard != null) {
            tenantGuard.assertAccessible(resourceTenantId);
        }
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
                current.tenantId(),
                current.name(),
                current.description(),
                current.ownerUnitId(),
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
                current.createdBy(),
                current.updatedBy(),
                current.createdAt(),
                Instant.now(),
                current.kbType(),
                current.bizScope(),
                current.datasetMode(),
                current.defaultDatasetId(),
                current.datasetCount(),
                current.metadataJson()
        ));
    }

    private KnowledgeChunkVector saveEmbeddingIfConfigured(KnowledgeBase knowledgeBase, KnowledgeChunk chunk) {
        if (!shouldUseVector(knowledgeBase)) {
            return null;
        }
        KnowledgeChunkVector vector = store.saveChunkVector(new KnowledgeChunkVector(
                chunk.id(),
                chunk.knowledgeBaseId(),
                chunk.documentId(),
                knowledgeBase.embeddingModelId(),
                embeddingClient.embed(knowledgeBase.embeddingModelId(), knowledgeBase.embeddingModelId(), chunk.content()),
                Instant.now()
        ));
        externalVectorStore(knowledgeBase)
                .ifPresent(config -> elasticsearchVectorStoreClient.upsertChunk(config, chunk, vector));
        return vector;
    }

    private KnowledgeDocument saveDocumentFromPreviews(
            String knowledgeBaseId,
            String name,
            List<KnowledgeChunkPreview> previews,
            String datasetType,
            String tags,
            String category,
            String source,
            int rowCount,
            String parserType,
            KnowledgeSplitRequest splitRequest,
            String rawContent
    ) {
        return saveDocumentFromPreviews(
                knowledgeBaseId,
                "doc_" + UUID.randomUUID(),
                name,
                previews,
                datasetType,
                tags,
                category,
                source,
                rowCount,
                parserType,
                splitRequest,
                rawContent,
                null,
                null
        );
    }

    private KnowledgeDocument saveDocumentFromPreviews(
            String knowledgeBaseId,
            String documentId,
            String name,
            List<KnowledgeChunkPreview> previews,
            String datasetType,
            String tags,
            String category,
            String source,
            int rowCount,
            String parserType,
            KnowledgeSplitRequest splitRequest,
            String rawContent,
            String storagePath
    ) {
        return saveDocumentFromPreviews(
                knowledgeBaseId,
                documentId,
                name,
                previews,
                datasetType,
                tags,
                category,
                source,
                rowCount,
                parserType,
                splitRequest,
                rawContent,
                storagePath,
                null
        );
    }

    private KnowledgeDocument saveDocumentFromPreviews(
            String knowledgeBaseId,
            String documentId,
            String name,
            List<KnowledgeChunkPreview> previews,
            String datasetType,
            String tags,
            String category,
            String source,
            int rowCount,
            String parserType,
            KnowledgeSplitRequest splitRequest,
            String rawContent,
            String storagePath,
            String datasetId
    ) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        KnowledgeDocument document = store.saveDocument(new KnowledgeDocument(
                documentId,
                knowledgeBaseId,
                name,
                previews.size(),
                Instant.now(),
                datasetType,
                "READY",
                blankToNull(tags),
                blankToNull(category),
                blankToNull(source),
                rowCount,
                parserType,
                splitRequest.effectiveSplitterType(),
                splitConfig(splitRequest),
                rawContent,
                null,
                storagePath,
                resolveDatasetId(knowledgeBase, datasetId),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        ));
        saveChunksForDocument(knowledgeBase, document, previews);
        return document;
    }

    private void saveChunksForDocument(KnowledgeBase knowledgeBase, KnowledgeDocument document, List<KnowledgeChunkPreview> previews) {
        for (KnowledgeChunkPreview preview : previews) {
            KnowledgeChunk chunk = store.saveChunk(new KnowledgeChunk(
                    "chunk_" + UUID.randomUUID(),
                    document.knowledgeBaseId(),
                    document.id(),
                    document.name(),
                    preview.content(),
                    preview.index(),
                    true,
                    preview.tokenEstimate()
            ).withDatasetContext(
                    document.datasetId(),
                    document.sourceIndexId(),
                    document.topicId(),
                    document.sourceRefId(),
                    document.materialSourceType(),
                    document.materialType(),
                    document.sourceArchiveFileId()
            ));
            saveEmbeddingIfConfigured(knowledgeBase, chunk);
        }
    }

    public int countChunksByDataset(String knowledgeBaseId, String datasetId) {
        return (int) store.listChunks(knowledgeBaseId).stream()
                .filter(chunk -> matchesDataset(chunk, datasetId))
                .count();
    }

    public void updateDatasetCount(String knowledgeBaseId, int datasetCount) {
        KnowledgeBase current = getKnowledgeBase(knowledgeBaseId);
        store.saveKnowledgeBase(new KnowledgeBase(
                current.id(), current.tenantId(), current.name(), current.description(), current.ownerUnitId(),
                current.embeddingModelId(), current.vectorStoreConfigId(), current.vectorDimension(), current.splitterType(),
                current.chunkSize(), current.chunkOverlap(), current.retrievalMode(), current.topK(), current.status(),
                current.documentCount(), current.chunkCount(), current.createdBy(), current.updatedBy(),
                current.createdAt(), Instant.now(), current.kbType(), current.bizScope(), current.datasetMode(),
                current.defaultDatasetId(), datasetCount, current.metadataJson()
        ));
    }

    public KnowledgeDocument indexSourceMaterial(String sourceIndexId) {
        if (sourceIndexMapper == null) {
            throw new IllegalStateException("来源索引存储不可用");
        }
        com.mw.ai.agi.knowledge.persistence.KnowledgeSourceIndexEntity entity = sourceIndexMapper.selectById(sourceIndexId);
        if (entity == null) {
            throw new IllegalArgumentException("Knowledge source index not found: " + sourceIndexId);
        }
        KnowledgeBase knowledgeBase = getKnowledgeBase(entity.getKnowledgeBaseId());
        String content = extractSourceContent(entity.getMetadataSnapshot(), entity.getSourceTitleSnapshot());
        String documentId = entity.getDocumentId();
        KnowledgeDocument document;
        Instant now = Instant.now();
        if (documentId == null || documentId.isBlank()) {
            document = new KnowledgeDocument(
                    "doc_" + UUID.randomUUID(),
                    entity.getKnowledgeBaseId(),
                    entity.getSourceTitleSnapshot(),
                    0,
                    now,
                    "TEXT_DOCUMENT",
                    "PROCESSING",
                    null,
                    null,
                    entity.getSourceSystem(),
                    0,
                    "TEXT",
                    knowledgeBase.splitterType(),
                    "{}",
                    content,
                    null,
                    null,
                    entity.getDatasetId(),
                    entity.getId(),
                    entity.getTopicId(),
                    "ARCHIVE_TOPIC_MATERIAL",
                    entity.getSourceSystem(),
                    entity.getSourceType(),
                    entity.getSourceRefId(),
                    entity.getMaterialSourceType(),
                    entity.getMaterialType(),
                    entity.getSourceArchiveFileId(),
                    entity.getSourceVersion(),
                    entity.getSourceTitleSnapshot(),
                    entity.getMetadataSnapshot(),
                    summarize(content),
                    null,
                    knowledgeBase.ownerUnitId(),
                    now
            );
        } else {
            document = store.listDocuments(entity.getKnowledgeBaseId()).stream()
                    .filter(item -> item.id().equals(documentId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
            externalVectorStore(knowledgeBase)
                    .ifPresent(config -> elasticsearchVectorStoreClient.deleteDocument(config, entity.getKnowledgeBaseId(), documentId));
            store.deleteChunkVectors(entity.getKnowledgeBaseId(), documentId);
            store.deleteChunks(entity.getKnowledgeBaseId(), documentId);
            document = document.withProcessingStatus("PROCESSING", null);
        }
        store.saveDocument(document);
        List<KnowledgeChunkPreview> previews = splitter.preview(
                content,
                knowledgeBase.splitterType(),
                knowledgeBase.chunkSize(),
                knowledgeBase.chunkOverlap()
        );
        document = document.withChunkCount(previews.size()).withProcessingStatus("READY", null);
        store.saveDocument(document);
        saveChunksForDocument(knowledgeBase, document, previews);
        entity.setDocumentId(document.id());
        entity.setIndexStatus("READY");
        entity.setLastIndexTime(now);
        entity.setUpdatedAt(now);
        entity.setErrorMessage(null);
        sourceIndexMapper.updateById(entity);
        refreshKnowledgeBaseStats(entity.getKnowledgeBaseId());
        return document;
    }

    public List<com.mw.ai.agi.knowledge.domain.KnowledgeRetrievalItem> retrieve(
            String knowledgeBaseId,
            String datasetId,
            String query,
            int topK,
            Map<String, Object> context
    ) {
        return retrieve(knowledgeBaseId, datasetId, query, topK, null, null, context);
    }

    public List<com.mw.ai.agi.knowledge.domain.KnowledgeRetrievalItem> retrieve(
            String knowledgeBaseId,
            String datasetId,
            String query,
            int topK,
            String retrievalMode,
            Map<String, Object> filters,
            Map<String, Object> context
    ) {
        KnowledgeSearchOptions options = KnowledgeSearchOptions.of(retrievalMode, filters);
        Map<String, KnowledgeChunk> chunksById = store.listChunks(knowledgeBaseId).stream()
                .collect(Collectors.toMap(KnowledgeChunk::id, chunk -> chunk, (left, right) -> left));
        return search(knowledgeBaseId, datasetId, query, topK, context, options).stream()
                .map(result -> {
                    KnowledgeChunk chunk = chunksById.get(result.id());
                    return new com.mw.ai.agi.knowledge.domain.KnowledgeRetrievalItem(
                            result.id(),
                            chunk == null ? null : chunk.documentId(),
                            chunk == null ? null : chunk.sourceIndexId(),
                            result.content(),
                            result.score(),
                            result.documentName(),
                            chunk == null ? null : chunk.sourceRefId(),
                            chunk == null ? null : chunk.sourceArchiveFileId(),
                            chunk == null ? null : chunk.sourcePage(),
                            chunk == null ? null : chunk.citationText(),
                            parseMetadata(chunk)
                    );
                })
                .toList();
    }

    private Map<String, Object> parseMetadata(KnowledgeChunk chunk) {
        if (chunk == null || chunk.metadataJson() == null || chunk.metadataJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(chunk.metadataJson(), Map.class);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String resolveDefaultDatasetId(KnowledgeBase knowledgeBase) {
        if (knowledgeBase.defaultDatasetId() != null && !knowledgeBase.defaultDatasetId().isBlank()) {
            return knowledgeBase.defaultDatasetId();
        }
        return "ds_default_" + knowledgeBase.id();
    }

    private String resolveDatasetId(KnowledgeBase knowledgeBase, String datasetId) {
        if (datasetId != null && !datasetId.isBlank()) {
            return datasetId;
        }
        return resolveDefaultDatasetId(knowledgeBase);
    }

    private boolean matchesDataset(KnowledgeChunk chunk, String datasetId) {
        if (datasetId == null || datasetId.isBlank()) {
            return true;
        }
        String chunkDatasetId = chunk.datasetId();
        if (chunkDatasetId == null || chunkDatasetId.isBlank()) {
            return datasetId.equals("ds_default_" + chunk.knowledgeBaseId());
        }
        return datasetId.equals(chunkDatasetId);
    }

    private String extractSourceContent(String metadataSnapshot, String title) {
        if (metadataSnapshot != null && !metadataSnapshot.isBlank()) {
            try {
                Map<?, ?> metadata = objectMapper.readValue(metadataSnapshot, Map.class);
                Object contentText = metadata.get("contentText");
                if (contentText != null && !String.valueOf(contentText).isBlank()) {
                    return String.valueOf(contentText);
                }
                Object summary = metadata.get("summary");
                if (summary != null && !String.valueOf(summary).isBlank()) {
                    return String.valueOf(summary);
                }
            } catch (Exception ignored) {
            }
        }
        return title == null ? "" : title;
    }

    private String summarize(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200) + "...";
    }

    private String manualEntryText(ManualDatasetEntry entry) {
        if (entry == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendLabeled(builder, "Title", entry.title());
        appendLabeled(builder, "Category", entry.category());
        appendLabeled(builder, "Tags", entry.tags());
        appendLabeled(builder, "Source", entry.source());
        appendLabeled(builder, "Body", entry.content());
        return builder.toString().trim();
    }

    private String manualDatasetName(List<ManualDatasetEntry> entries) {
        return entries.stream()
                .map(ManualDatasetEntry::title)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse("Manual dataset");
    }

    private String combinedManualValue(List<ManualDatasetEntry> entries, java.util.function.Function<ManualDatasetEntry, String> extractor) {
        return entries.stream()
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private int estimateManualTokens(String content) {
        return Math.max(1, (int) Math.ceil(content.length() / 4.0));
    }

    private void appendLabeled(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(label).append(": ").append(value.trim());
        }
    }

    private KnowledgeDocument withStatus(KnowledgeDocument document, String status, String errorMessage) {
        return document.withProcessingStatus(status, errorMessage);
    }

    private KnowledgeDocument withChunkCount(KnowledgeDocument document, int chunkCount) {
        return document.withChunkCount(chunkCount);
    }

    private KnowledgeSplitRequest splitRequestFrom(KnowledgeDocument document) {
        int chunkSize = intConfig(document.splitterConfig(), "chunkSize", 200);
        String separator = stringConfig(document.splitterConfig(), "separator");
        return new KnowledgeSplitRequest(defaultString(document.splitterType(), "FIXED_LENGTH"), chunkSize, separator);
    }

    private String splitConfig(KnowledgeSplitRequest splitRequest) {
        String separator = splitRequest.separator() == null ? "" : splitRequest.separator()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "{\"chunkSize\":" + splitRequest.effectiveChunkSize() + ",\"separator\":\"" + separator + "\"}";
    }

    private int intConfig(String json, String key, int fallback) {
        String value = stringConfig(json, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String stringConfig(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        java.util.regex.Matcher quoted = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (quoted.find()) {
            return quoted.group(1);
        }
        java.util.regex.Matcher number = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        return number.find() ? number.group(1) : null;
    }

    private String sheetName(String fileName) {
        String normalized = fileName == null || fileName.isBlank() ? "Sheet1" : fileName.trim();
        int dot = normalized.lastIndexOf('.');
        return dot > 0 ? normalized.substring(0, dot) : normalized;
    }

    public int backfillEmbeddingsIfConfigured(String knowledgeBaseId) {
        return backfillEmbeddingsIfConfigured(getKnowledgeBase(knowledgeBaseId));
    }

    private int backfillEmbeddingsIfConfigured(KnowledgeBase knowledgeBase) {
        if (!shouldUseVector(knowledgeBase)) {
            return 0;
        }
        Set<String> vectorChunkIds = vectorsByChunkId(knowledgeBase.id()).keySet();
        int created = 0;
        List<KnowledgeChunk> missingChunks = store.listChunks(knowledgeBase.id()).stream()
                .filter(chunk -> !vectorChunkIds.contains(chunk.id()))
                .toList();
        for (int start = 0; start < missingChunks.size(); start += EMBEDDING_BACKFILL_BATCH_SIZE) {
            List<KnowledgeChunk> batch = missingChunks.subList(start, Math.min(start + EMBEDDING_BACKFILL_BATCH_SIZE, missingChunks.size()));
            List<List<Double>> embeddings = embeddingClient.embedAll(
                    knowledgeBase.embeddingModelId(),
                    knowledgeBase.embeddingModelId(),
                    batch.stream().map(KnowledgeChunk::content).toList()
            );
            if (embeddings.size() != batch.size()) {
                throw new IllegalStateException("Embedding provider returned " + embeddings.size()
                        + " vectors for " + batch.size() + " chunks");
            }
            for (int index = 0; index < batch.size(); index++) {
                KnowledgeChunk chunk = batch.get(index);
                KnowledgeChunkVector vector = store.saveChunkVector(new KnowledgeChunkVector(
                        chunk.id(),
                        chunk.knowledgeBaseId(),
                        chunk.documentId(),
                        knowledgeBase.embeddingModelId(),
                        embeddings.get(index),
                        Instant.now()
                ));
                externalVectorStore(knowledgeBase)
                        .ifPresent(config -> elasticsearchVectorStoreClient.upsertChunk(config, chunk, vector));
                created++;
            }
        }
        return created;
    }

    private Optional<VectorStoreConfig> externalVectorStore(KnowledgeBase knowledgeBase) {
        if (knowledgeBase.vectorStoreConfigId() == null || knowledgeBase.vectorStoreConfigId().isBlank()) {
            return Optional.empty();
        }
        return vectorStoreConfigStore.findById(knowledgeBase.vectorStoreConfigId())
                .filter(VectorStoreConfig::enabled)
                .filter(config -> "ELASTICSEARCH".equalsIgnoreCase(config.storeType()))
                .filter(config -> config.endpoint() != null && !config.endpoint().isBlank());
    }

    private boolean shouldUseVector(KnowledgeBase knowledgeBase) {
        return shouldUseVector(knowledgeBase, knowledgeBase.retrievalMode());
    }

    private boolean shouldUseVector(KnowledgeBase knowledgeBase, String retrievalMode) {
        String effectiveMode = retrievalMode == null || retrievalMode.isBlank()
                ? DEFAULT_RETRIEVAL_MODE
                : retrievalMode.trim().toUpperCase(Locale.ROOT);
        return knowledgeBase.embeddingModelId() != null && !knowledgeBase.embeddingModelId().isBlank()
                && ("VECTOR".equalsIgnoreCase(effectiveMode) || "HYBRID".equalsIgnoreCase(effectiveMode));
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
        return keywordScore * 1000;
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
        if (normalized.isBlank()) {
            return terms;
        }
        for (String term : normalized.split("[\\s,\\uFF0C\\u3002\\uFF1B;:\\uFF1A]+")) {
            if (!term.isBlank()) {
                terms.add(term);
                appendCjkTerms(term, terms);
            }
        }
        return terms;
    }

    private void appendCjkTerms(String text, Set<String> terms) {
        StringBuilder run = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (isCjk(ch)) {
                run.append(ch);
                continue;
            }
            flushCjkRun(run, terms);
            run.setLength(0);
        }
        flushCjkRun(run, terms);
    }

    private void flushCjkRun(StringBuilder run, Set<String> terms) {
        if (run.length() < 2) {
            return;
        }
        String segment = run.toString();
        terms.add(segment);
        for (int index = 0; index < segment.length() - 1; index++) {
            terms.add(segment.substring(index, index + 2));
        }
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private int score(String content, Set<String> terms) {
        String normalized = content.toLowerCase(Locale.ROOT);
        int score = 0;
        boolean matchedSignificantTerm = false;
        for (String term : terms) {
            if (term.isBlank() || !normalized.contains(term)) {
                continue;
            }
            if (term.length() >= 2) {
                matchedSignificantTerm = true;
                score += term.length() * 10;
            }
        }
        return matchedSignificantTerm ? score : 0;
    }

    private List<KnowledgeSearchResult> applyRelevanceThreshold(List<KnowledgeSearchResult> results) {
        if (results.isEmpty()) {
            return results;
        }
        int maxScore = results.stream().mapToInt(KnowledgeSearchResult::score).max().orElse(0);
        if (maxScore < DEFAULT_MIN_RELEVANCE_SCORE) {
            return List.of();
        }
        return results.stream()
                .filter(result -> result.score() >= DEFAULT_MIN_RELEVANCE_SCORE)
                .toList();
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

    public record ManualDatasetEntry(
            String title,
            String content,
            String tags,
            String category,
            String source
    ) {
    }
}