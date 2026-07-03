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
import com.mw.ai.agi.knowledge.chunking.ChunkQualityGate;
import com.mw.ai.agi.knowledge.vector.ElasticsearchVectorStoreProvider;
import com.mw.ai.agi.knowledge.vector.VectorStoreProvider;
import com.mw.ai.agi.knowledge.vector.VectorStoreProviderRegistry;
import com.mw.ai.agi.knowledge.vector.VectorStoreSearchRequest;
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
import java.util.LinkedHashMap;
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
    private static final org.slf4j.Logger QUALITY_LOG =
            org.slf4j.LoggerFactory.getLogger("knowledge.chunking.quality");
    private static final int DEFAULT_VECTOR_DIMENSION = 1536;
    private static final int DEFAULT_MIN_RELEVANCE_SCORE = 350;
    private static final int EMBEDDING_BACKFILL_BATCH_SIZE = 16;
    private static final String DEFAULT_SPLITTER_TYPE = "SIMPLE_TEXT";
    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;
    private static final String DEFAULT_RETRIEVAL_MODE = "HYBRID";
    private static final int DEFAULT_TOP_K = 3;
    private final com.mw.ai.agi.knowledge.chunking.TokenCounter tokenCounter;
    private final KnowledgeStore store;
    private final EmbeddingClient embeddingClient;
    private final DocumentTextExtractor documentTextExtractor;
    private final KnowledgeDocumentSplitter documentSplitter;
    private final TableDocumentParser tableDocumentParser;
    private final VectorStoreConfigStore vectorStoreConfigStore;
    private final VectorStoreProviderRegistry vectorStoreProviders;
    private final AssetGrantService assetGrantService;
    private final TenantBusinessGuard tenantGuard;
    private final KnowledgeDocumentFileStorage knowledgeDocumentFileStorage;
    private final KnowledgeContextAssembler contextAssembler;
    private final KnowledgeReranker knowledgeReranker;
    private final DocumentStructureQualityAnalyzer structureQualityAnalyzer =
            new DocumentStructureQualityAnalyzer();
    private final ChunkQualityGate chunkQualityGate = new ChunkQualityGate();
    private KnowledgeDatasetService datasetService;
    private com.mw.ai.agi.knowledge.persistence.KnowledgeSourceIndexMapper sourceIndexMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeBaseService() {
        this(
                new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(),
                new InMemoryKnowledgeStore(),
                new LocalEmbeddingClient()
        );
    }

    public KnowledgeBaseService(
            com.mw.ai.agi.knowledge.chunking.TokenCounter tokenCounter,
            KnowledgeStore store
    ) {
        this(tokenCounter, store, new LocalEmbeddingClient());
    }

    public KnowledgeBaseService(
            com.mw.ai.agi.knowledge.chunking.TokenCounter tokenCounter,
            KnowledgeStore store,
            EmbeddingClient embeddingClient
    ) {
        this(
                tokenCounter,
                store,
                embeddingClient,
                new DocumentTextExtractor(),
                new KnowledgeDocumentSplitter(tokenCounter, embeddingClient),
                new TableDocumentParser(),
                new InMemoryVectorStoreConfigStore(),
                new ElasticsearchVectorStoreClient(new ObjectMapper()),
                null,
                null,
                new HeuristicKnowledgeReranker(),
                new KnowledgeContextAssembler(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter()),
                new KnowledgeDocumentFileStorage(AgiStorageSettingsService.withDefaults(
                        new com.mw.ai.agi.config.AgiStorageProperties(),
                        new ObjectMapper()
                ))
        );
    }

    public KnowledgeBaseService(
            com.mw.ai.agi.knowledge.chunking.TokenCounter tokenCounter,
            KnowledgeStore store,
            EmbeddingClient embeddingClient,
            DocumentTextExtractor documentTextExtractor
    ) {
        this(
                tokenCounter,
                store,
                embeddingClient,
                documentTextExtractor,
                new KnowledgeDocumentSplitter(tokenCounter, embeddingClient),
                new TableDocumentParser(),
                new InMemoryVectorStoreConfigStore(),
                new ElasticsearchVectorStoreClient(new ObjectMapper()),
                null,
                null,
                new HeuristicKnowledgeReranker(),
                new KnowledgeContextAssembler(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter()),
                new KnowledgeDocumentFileStorage(AgiStorageSettingsService.withDefaults(
                        new com.mw.ai.agi.config.AgiStorageProperties(),
                        new ObjectMapper()
                ))
        );
    }

    @Autowired
    public KnowledgeBaseService(
            com.mw.ai.agi.knowledge.chunking.TokenCounter tokenCounter,
            KnowledgeStore store,
            EmbeddingClient embeddingClient,
            DocumentTextExtractor documentTextExtractor,
            KnowledgeDocumentSplitter documentSplitter,
            TableDocumentParser tableDocumentParser,
            VectorStoreConfigStore vectorStoreConfigStore,
            VectorStoreProviderRegistry vectorStoreProviders,
            AssetGrantService assetGrantService,
            TenantBusinessGuard tenantGuard,
            KnowledgeReranker knowledgeReranker,
            KnowledgeContextAssembler contextAssembler,
            KnowledgeDocumentFileStorage knowledgeDocumentFileStorage
    ) {
        this.tokenCounter = tokenCounter;
        this.store = store;
        this.embeddingClient = embeddingClient;
        this.documentTextExtractor = documentTextExtractor;
        this.documentSplitter = documentSplitter;
        this.tableDocumentParser = tableDocumentParser;
        this.vectorStoreConfigStore = vectorStoreConfigStore;
        this.vectorStoreProviders = vectorStoreProviders;
        this.assetGrantService = assetGrantService;
        this.tenantGuard = tenantGuard;
        this.knowledgeReranker = knowledgeReranker;
        this.contextAssembler = contextAssembler;
        this.knowledgeDocumentFileStorage = knowledgeDocumentFileStorage;
    }

    public KnowledgeBaseService(
            com.mw.ai.agi.knowledge.chunking.TokenCounter tokenCounter,
            KnowledgeStore store,
            EmbeddingClient embeddingClient,
            DocumentTextExtractor documentTextExtractor,
            KnowledgeDocumentSplitter documentSplitter,
            TableDocumentParser tableDocumentParser,
            VectorStoreConfigStore vectorStoreConfigStore,
            ElasticsearchVectorStoreClient elasticsearchVectorStoreClient,
            AssetGrantService assetGrantService,
            TenantBusinessGuard tenantGuard,
            KnowledgeReranker knowledgeReranker,
            KnowledgeContextAssembler contextAssembler,
            KnowledgeDocumentFileStorage knowledgeDocumentFileStorage
    ) {
        this(
                tokenCounter, store, embeddingClient, documentTextExtractor, documentSplitter,
                tableDocumentParser, vectorStoreConfigStore,
                new VectorStoreProviderRegistry(List.of(
                        new ElasticsearchVectorStoreProvider(elasticsearchVectorStoreClient)
                )),
                assetGrantService, tenantGuard, knowledgeReranker, contextAssembler,
                knowledgeDocumentFileStorage
        );
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
        return create(
                name, description, ownerUnitId, embeddingModelId,
                vectorStoreConfigId, vectorDimension, splitterType, chunkSize,
                chunkOverlap, retrievalMode, topK, kbType, datasetMode, null
        );
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
            String datasetMode,
            Double semanticSimilarityThreshold
    ) {
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        String effectiveKbType = defaultString(kbType, "NORMAL");
        String effectiveDatasetMode = "MULTI";
        int effectiveVectorDimension = normalizeVectorDimension(vectorDimension);
        validateVectorStoreDimension(vectorStoreConfigId, effectiveVectorDimension);
        KnowledgeBase knowledgeBase = new KnowledgeBase(
                "kb_" + UUID.randomUUID(),
                currentTenantId(),
                name,
                description,
                blankToNull(ownerUnitId),
                blankToNull(embeddingModelId),
                blankToNull(vectorStoreConfigId),
                effectiveVectorDimension,
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
                null,
                normalizeSemanticThreshold(semanticSimilarityThreshold, 0.78)
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
                    saved.kbType(), saved.bizScope(), effectiveDatasetMode,
                    dataset.id(), 1, saved.metadataJson(),
                    saved.semanticSimilarityThreshold()
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
        return update(
                id, name, description, ownerUnitId, embeddingModelId,
                vectorStoreConfigId, vectorDimension, splitterType, chunkSize,
                chunkOverlap, retrievalMode, topK, null
        );
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
            int topK,
            Double semanticSimilarityThreshold
    ) {
        KnowledgeBase current = getKnowledgeBase(id);
        String operator = OperatorContext.currentUserId();
        int effectiveVectorDimension = vectorDimension <= 0 ? current.vectorDimension() : vectorDimension;
        validateVectorStoreDimension(vectorStoreConfigId, effectiveVectorDimension);
        KnowledgeBase updated = new KnowledgeBase(
                current.id(),
                current.tenantId(),
                defaultString(name, current.name()),
                description,
                blankToNull(ownerUnitId != null && !ownerUnitId.isBlank() ? ownerUnitId : current.ownerUnitId()),
                blankToNull(embeddingModelId),
                blankToNull(vectorStoreConfigId),
                effectiveVectorDimension,
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
                current.metadataJson(),
                normalizeSemanticThreshold(
                        semanticSimilarityThreshold,
                        current.semanticSimilarityThreshold()
                )
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
                .ifPresent(config -> provider(config).deleteKnowledgeBase(config, id));
        store.deleteKnowledgeBase(id);
    }

    public List<KnowledgeChunkPreview> previewChunks(String content, String splitterType, int chunkSize, int chunkOverlap) {
        return previewChunks(content, splitterType, chunkSize, chunkOverlap, null);
    }

    public List<KnowledgeChunkPreview> previewChunks(
            String content,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            Double semanticSimilarityThreshold
    ) {
        return documentSplitter.splitText(
                content,
                new KnowledgeSplitRequest(
                        splitterType, chunkSize, chunkOverlap, null, null,
                        semanticSimilarityThreshold
                )
        );
    }

    public UploadedDocumentPreview previewDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            String splitterType,
            int chunkSize,
            int chunkOverlap
    ) {
        return previewDocumentFile(
                fileName, contentType, inputStream, splitterType, chunkSize,
                chunkOverlap, null
        );
    }

    public UploadedDocumentPreview previewDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            Double semanticSimilarityThreshold
    ) {
        return previewDocumentFile(
                fileName, contentType, inputStream, splitterType, chunkSize,
                chunkOverlap, semanticSimilarityThreshold, null
        );
    }

    public UploadedDocumentPreview previewDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            Double semanticSimilarityThreshold,
            String knowledgeBaseId
    ) {
        String content = documentTextExtractor.extract(fileName, contentType, inputStream);
        KnowledgeSplitRequest request = new KnowledgeSplitRequest(
                splitterType, chunkSize, chunkOverlap, null, null,
                semanticSimilarityThreshold
        );
        KnowledgeSplitRequest effectiveRequest = knowledgeBaseId == null || knowledgeBaseId.isBlank()
                ? request
                : effectiveSplitRequest(getKnowledgeBase(knowledgeBaseId), request);
        List<KnowledgeChunkPreview> chunks = documentSplitter.splitDocument(
                fileName,
                content,
                effectiveRequest
        );
        return new UploadedDocumentPreview(
                fileName,
                content.length(),
                chunks,
                structureQualityAnalyzer.analyze(fileName, content)
        );
    }

    public UploadedDocumentPreview previewTextDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest
    ) {
        return previewTextDocumentFile(fileName, contentType, inputStream, splitRequest, null);
    }

    public UploadedDocumentPreview previewTextDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest,
            String knowledgeBaseId
    ) {
        String content = documentTextExtractor.extract(fileName, contentType, inputStream);
        KnowledgeSplitRequest effectiveRequest = knowledgeBaseId == null || knowledgeBaseId.isBlank()
                ? splitRequest
                : effectiveSplitRequest(getKnowledgeBase(knowledgeBaseId), splitRequest);
        List<KnowledgeChunkPreview> chunks =
                documentSplitter.splitDocument(fileName, content, effectiveRequest);
        return new UploadedDocumentPreview(
                fileName,
                content.length(),
                chunks,
                structureQualityAnalyzer.analyze(fileName, content)
        );
    }

    public UploadedDocumentPreview previewTableDocumentFile(
            String fileName,
            String contentType,
            java.io.InputStream inputStream,
            KnowledgeSplitRequest splitRequest
    ) {
        String content = documentTextExtractor.extract(fileName, contentType, inputStream);
        List<KnowledgeChunkPreview> chunks = documentSplitter.splitDocument(fileName, content, splitRequest);
        return new UploadedDocumentPreview(
                fileName,
                content.length(),
                chunks,
                structureQualityAnalyzer.analyze(fileName, content)
        );
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
        return addDocument(
                knowledgeBaseId, name, content, splitterType, chunkSize,
                chunkOverlap, datasetId, null
        );
    }

    public KnowledgeDocument addDocument(
            String knowledgeBaseId,
            String name,
            String content,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String datasetId,
            Double semanticSimilarityThreshold
    ) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        String effectiveSplitterType = defaultString(splitterType, knowledgeBase.splitterType());
        int effectiveChunkSize = chunkSize <= 0 ? knowledgeBase.chunkSize() : chunkSize;
        int effectiveChunkOverlap = chunkOverlap < 0 ? knowledgeBase.chunkOverlap() : chunkOverlap;
        List<KnowledgeChunkPreview> previews = documentSplitter.splitDocument(
                name,
                content,
                new KnowledgeSplitRequest(
                        effectiveSplitterType,
                        effectiveChunkSize,
                        effectiveChunkOverlap,
                        null,
                        knowledgeBase.embeddingModelId(),
                        normalizeSemanticThreshold(
                                semanticSimilarityThreshold,
                                knowledgeBase.semanticSimilarityThreshold()
                        )
                )
        );
        String effectiveDatasetId = resolveDatasetId(knowledgeBase, datasetId);
        KnowledgeDocument document = new KnowledgeDocument(
                "doc_" + UUID.randomUUID(),
                knowledgeBaseId,
                name,
                previews.size(),
                Instant.now()
        ).withDatasetId(effectiveDatasetId);
        store.saveDocument(document);
        saveChunksForDocument(knowledgeBase, document, previews);
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
            KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
            KnowledgeSplitRequest effectiveSplitRequest = effectiveSplitRequest(knowledgeBase, splitRequest);
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
                    documentSplitter.splitDocument(fileName, content, effectiveSplitRequest),
                    "TEXT_DOCUMENT",
                    null,
                    null,
                    fileName,
                    0,
                    "TEXT",
                    effectiveSplitRequest,
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
            KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
            KnowledgeSplitRequest effectiveSplitRequest = effectiveSplitRequest(knowledgeBase, splitRequest);
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
            List<KnowledgeChunkPreview> previews =
                    documentSplitter.splitDocument(fileName, content, effectiveSplitRequest);
            int rowCount = (int) previews.stream()
                    .filter(preview -> "TABLE_ROW".equals(preview.chunkType()))
                    .count();
            KnowledgeDocument document = saveDocumentFromPreviews(
                    knowledgeBaseId,
                    documentId,
                    fileName,
                    previews,
                    "TABLE_DOCUMENT",
                    null,
                    null,
                    fileName,
                    rowCount,
                    "TABLE",
                    effectiveSplitRequest,
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
        List<KnowledgeDocument> documents = store.listDocuments(knowledgeBaseId).stream()
                .map(document -> knowledgeDocumentFileStorage.resolveExisting(document.storagePath()).isPresent()
                        ? document
                        : document.withStoragePath(null))
                .toList();
        if (datasetId == null || datasetId.isBlank()) {
            return documents;
        }
        String defaultDatasetId = resolveDefaultDatasetId(knowledgeBase);
        return documents.stream()
                .filter(document -> matchesDocumentDataset(document, datasetId, defaultDatasetId))
                .toList();
    }

    public OriginalDocumentFile originalDocumentFile(
            String knowledgeBaseId,
            String documentId,
            Map<String, Object> context
    ) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        assertKnowledgeBaseUseAllowed(knowledgeBase, context == null ? Map.of() : context);
        KnowledgeDocument document = store.listDocuments(knowledgeBaseId).stream()
                .filter(item -> item.id().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
        java.nio.file.Path path = knowledgeDocumentFileStorage.resolveExisting(document.storagePath())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Original document file is not available"
                ));
        String contentType;
        try {
            contentType = java.nio.file.Files.probeContentType(path);
        } catch (java.io.IOException exception) {
            contentType = null;
        }
        return new OriginalDocumentFile(
                path,
                document.name(),
                contentType == null ? "application/octet-stream" : contentType
        );
    }

    public record OriginalDocumentFile(
            java.nio.file.Path path,
            String fileName,
            String contentType
    ) {
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
                KnowledgeChunk updated = store.saveChunk(current.withContent(
                        updatedContent,
                        enabled,
                        tokenCounter.count(updatedContent, null)
                ));
                if (!"PARENT".equalsIgnoreCase(updated.chunkLevel())) {
                    saveEmbeddingIfConfigured(getKnowledgeBase(knowledgeBaseId), updated);
                }
                return updated;
            }
        }
        throw new IllegalArgumentException("Knowledge chunk not found: " + chunkId);
    }

    public List<KnowledgeChunk> splitChunk(String knowledgeBaseId, String chunkId, int offset) {
        KnowledgeChunk current = requireChunk(knowledgeBaseId, chunkId);
        if (!isRetrievableChild(current)) {
            throw new IllegalArgumentException("Only child chunks can be split");
        }
        if (offset <= 0 || offset >= current.content().length()) {
            throw new IllegalArgumentException("Split offset must be inside the chunk content");
        }
        String leftContent = current.content().substring(0, offset).trim();
        String rightContent = current.content().substring(offset).trim();
        if (leftContent.isBlank() || rightContent.isBlank()) {
            throw new IllegalArgumentException("Split must produce two non-empty chunks");
        }
        KnowledgeChunk left = store.saveChunk(current.withContent(
                leftContent,
                current.enabled(),
                tokenCounter.count(leftContent, null)
        ));
        KnowledgeChunk right = store.saveChunk(new KnowledgeChunk(
                "chunk_" + UUID.randomUUID(),
                current.knowledgeBaseId(),
                current.documentId(),
                current.documentName(),
                rightContent,
                current.index() + 1,
                current.enabled(),
                tokenCounter.count(rightContent, null)
        ).withDatasetContext(
                current.datasetId(),
                current.sourceIndexId(),
                current.topicId(),
                current.sourceRefId(),
                current.materialSourceType(),
                current.materialType(),
                current.sourceArchiveFileId()
        ).withStructure(
                logicalChunkId(current.documentId(), "manual_" + UUID.randomUUID()),
                current.parentChunkId(),
                current.groupId(),
                "CHILD",
                current.sectionPath(),
                current.chunkTitle(),
                current.chunkType(),
                current.metadataJson()
        ));
        KnowledgeBase base = getKnowledgeBase(knowledgeBaseId);
        saveEmbeddingIfConfigured(base, left);
        saveEmbeddingIfConfigured(base, right);
        refreshParentChunk(knowledgeBaseId, current.parentChunkId());
        refreshKnowledgeBaseStats(knowledgeBaseId);
        return List.of(left, right);
    }

    public KnowledgeChunk mergeChunks(String knowledgeBaseId, List<String> chunkIds) {
        if (chunkIds == null || chunkIds.size() < 2) {
            throw new IllegalArgumentException("At least two chunks are required for merge");
        }
        List<KnowledgeChunk> chunks = chunkIds.stream()
                .map(id -> requireChunk(knowledgeBaseId, id))
                .sorted(Comparator.comparingInt(KnowledgeChunk::index))
                .toList();
        KnowledgeChunk first = chunks.get(0);
        if (chunks.stream().anyMatch(chunk -> !isRetrievableChild(chunk)
                || !first.documentId().equals(chunk.documentId())
                || !java.util.Objects.equals(first.parentChunkId(), chunk.parentChunkId()))) {
            throw new IllegalArgumentException("Chunks must be child chunks from the same document and parent");
        }
        String content = chunks.stream().map(KnowledgeChunk::content).collect(Collectors.joining("\n\n"));
        KnowledgeChunk merged = store.saveChunk(first.withContent(
                content,
                chunks.stream().anyMatch(KnowledgeChunk::enabled),
                tokenCounter.count(content, null)
        ));
        for (int index = 1; index < chunks.size(); index++) {
            store.deleteChunk(chunks.get(index).id());
        }
        saveEmbeddingIfConfigured(getKnowledgeBase(knowledgeBaseId), merged);
        refreshParentChunk(knowledgeBaseId, first.parentChunkId());
        refreshKnowledgeBaseStats(knowledgeBaseId);
        return merged;
    }

    public KnowledgeChunk adjustChunkStructure(
            String knowledgeBaseId,
            String chunkId,
            String sectionPath,
            String parentChunkId
    ) {
        KnowledgeChunk current = requireChunk(knowledgeBaseId, chunkId);
        if (!isRetrievableChild(current)) {
            throw new IllegalArgumentException("Only child chunks can be adjusted");
        }
        if (parentChunkId != null && !parentChunkId.isBlank()) {
            KnowledgeChunk parent = requireChunk(knowledgeBaseId, parentChunkId);
            if (!"PARENT".equalsIgnoreCase(parent.chunkLevel())
                    || !current.documentId().equals(parent.documentId())) {
                throw new IllegalArgumentException("Parent must belong to the same document");
            }
        }
        String oldParentId = current.parentChunkId();
        KnowledgeChunk adjusted = store.saveChunk(current.withStructure(
                current.logicalChunkId(),
                parentChunkId,
                current.groupId(),
                current.chunkLevel(),
                sectionPath,
                current.chunkTitle(),
                current.chunkType(),
                current.metadataJson()
        ));
        saveEmbeddingIfConfigured(getKnowledgeBase(knowledgeBaseId), adjusted);
        refreshParentChunk(knowledgeBaseId, oldParentId);
        refreshParentChunk(knowledgeBaseId, parentChunkId);
        return adjusted;
    }

    private KnowledgeChunk requireChunk(String knowledgeBaseId, String chunkId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        return store.listChunks(knowledgeBaseId).stream()
                .filter(chunk -> chunk.id().equals(chunkId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Knowledge chunk not found: " + chunkId));
    }

    private void refreshParentChunk(String knowledgeBaseId, String parentChunkId) {
        if (parentChunkId == null || parentChunkId.isBlank()) {
            return;
        }
        KnowledgeChunk parent = store.listChunks(knowledgeBaseId).stream()
                .filter(chunk -> chunk.id().equals(parentChunkId))
                .findFirst()
                .orElse(null);
        if (parent != null) {
            List<KnowledgeChunk> children = store.listChunks(knowledgeBaseId, parent.documentId()).stream()
                    .filter(this::isRetrievableChild)
                    .filter(chunk -> parentChunkId.equals(chunk.parentChunkId()))
                    .sorted(Comparator.comparingInt(KnowledgeChunk::index))
                    .toList();
            String content = children.stream().map(KnowledgeChunk::content).collect(Collectors.joining("\n\n"));
            store.saveChunk(parent.withContent(
                    content,
                    parent.enabled(),
                    tokenCounter.count(content, null)
            ));
        }
    }

    public void deleteDocument(String knowledgeBaseId, String documentId) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        externalVectorStore(knowledgeBase)
                .ifPresent(config -> provider(config).deleteDocument(config, knowledgeBaseId, documentId));
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
            KnowledgeSplitRequest splitRequest = splitRequestFrom(current)
                    .withEmbeddingModel(knowledgeBase.embeddingModelId());
            List<KnowledgeChunkPreview> previews =
                    documentSplitter.splitDocument(current.name(), current.rawContent(), splitRequest);
            externalVectorStore(knowledgeBase)
                    .ifPresent(config -> provider(config).deleteDocument(config, knowledgeBaseId, documentId));
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
            VectorStoreConfig config = externalStore.get();
            List<KnowledgeSearchResult> vectorResults = provider(config).search(
                            config,
                            new VectorStoreSearchRequest(
                                    knowledgeBaseId,
                                    effectiveDatasetId,
                                    queryEmbedding,
                                    "HYBRID".equalsIgnoreCase(effectiveRetrievalMode)
                                            ? Math.max(limit * 3, limit) : limit,
                                    0
                            )
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
                return assembleContext(
                        knowledgeBaseId,
                        rerank(knowledgeBaseId, query, applyRelevanceThreshold(vectorResults), limit),
                        limit
                );
            }
            return assembleContext(
                    knowledgeBaseId,
                    rerank(knowledgeBaseId, query, applyRelevanceThreshold(mergeSearchResults(
                            vectorResults,
                            localKeywordResults(knowledgeBaseId, effectiveDatasetId, terms, filters),
                            Math.max(limit * 3, 15)
                    )), limit),
                    limit
            );
        }
        Map<String, KnowledgeChunkVector> vectorsByChunkId = vectorsByChunkId(knowledgeBaseId);
        List<Double> queryEmbedding = shouldUseVector(knowledgeBase, effectiveRetrievalMode)
                ? embeddingClient.embed(knowledgeBase.embeddingModelId(), knowledgeBase.embeddingModelId(), query)
                : List.of();
        List<KnowledgeSearchResult> results = store.listChunks(knowledgeBaseId).stream()
                .filter(KnowledgeChunk::enabled)
                .filter(this::isRetrievableChild)
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
                .limit(Math.max(limit * 3, 15))
                .toList();
        return assembleContext(
                knowledgeBaseId,
                rerank(knowledgeBaseId, query, applyRelevanceThreshold(results), limit),
                limit
        );
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
                .filter(this::isRetrievableChild)
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
        return assembleContext(
                knowledgeBaseId,
                rerank(knowledgeBaseId, query, applyRelevanceThreshold(results), limit),
                limit
        );
    }

    private List<KnowledgeSearchResult> localKeywordResults(
            String knowledgeBaseId,
            String datasetId,
            Set<String> terms,
            KnowledgeRetrievalFilters filters
    ) {
        return store.listChunks(knowledgeBaseId).stream()
                .filter(KnowledgeChunk::enabled)
                .filter(this::isRetrievableChild)
                .filter(chunk -> matchesDataset(chunk, datasetId))
                .filter(filters::matches)
                .map(chunk -> new KnowledgeSearchResult(
                        chunk.id(),
                        chunk.documentName(),
                        chunk.content(),
                        keywordScore(chunk, terms) * 1000
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

    private List<KnowledgeSearchResult> assembleContext(
            String knowledgeBaseId,
            List<KnowledgeSearchResult> seeds,
            int limit
    ) {
        return contextAssembler.expand(
                seeds,
                store.listChunks(knowledgeBaseId),
                4000,
                limit
        );
    }

    private List<KnowledgeSearchResult> rerank(
            String knowledgeBaseId,
            String query,
            List<KnowledgeSearchResult> candidates,
            int limit
    ) {
        Map<String, KnowledgeChunk> chunksById = store.listChunks(knowledgeBaseId).stream()
                .collect(Collectors.toMap(KnowledgeChunk::id, chunk -> chunk, (left, right) -> left));
        return knowledgeReranker.rerank(query, candidates, chunksById).stream()
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
        int chunkCount = (int) store.listChunks(knowledgeBaseId).stream()
                .filter(this::isRetrievableChild)
                .count();
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
        String embeddingContent = embeddingContent(chunk);
        KnowledgeChunkVector vector = store.saveChunkVector(new KnowledgeChunkVector(
                chunk.id(),
                chunk.knowledgeBaseId(),
                chunk.documentId(),
                knowledgeBase.embeddingModelId(),
                embeddingClient.embed(knowledgeBase.embeddingModelId(), knowledgeBase.embeddingModelId(), embeddingContent),
                Instant.now()
        ));
        externalVectorStore(knowledgeBase)
                .ifPresent(config -> provider(config).upsertChunk(config, chunk, vector));
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
        chunkQualityGate.validate(previews, splitRequest.effectiveChunkSize());
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
        Map<String, List<KnowledgeChunkPreview>> parentGroups = new LinkedHashMap<>();
        for (KnowledgeChunkPreview preview : previews) {
            String parentKey = preview.parentChunkId() == null || preview.parentChunkId().isBlank()
                    ? "parent_" + preview.index()
                    : preview.parentChunkId();
            parentGroups.computeIfAbsent(parentKey, ignored -> new java.util.ArrayList<>()).add(preview);
        }

        Map<String, String> persistedParentIds = new HashMap<>();
        int parentIndex = previews.size();
        for (Map.Entry<String, List<KnowledgeChunkPreview>> entry : parentGroups.entrySet()) {
            List<KnowledgeChunkPreview> children = entry.getValue();
            KnowledgeChunkPreview first = children.get(0);
            String parentId = "chunk_" + UUID.randomUUID();
            String parentContent = children.stream()
                    .map(KnowledgeChunkPreview::content)
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.joining("\n\n"));
            KnowledgeChunk parent = new KnowledgeChunk(
                    parentId,
                    document.knowledgeBaseId(),
                    document.id(),
                    document.name(),
                    parentContent,
                    parentIndex++,
                    true,
                    tokenCounter.count(parentContent, null)
            ).withDatasetContext(
                    document.datasetId(),
                    document.sourceIndexId(),
                    document.topicId(),
                    document.sourceRefId(),
                    document.materialSourceType(),
                    document.materialType(),
                    document.sourceArchiveFileId()
            ).withStructure(
                    logicalChunkId(document.id(), entry.getKey()),
                    null,
                    normalizeGroupId(document.id(), first.groupId()),
                    "PARENT",
                    sectionPath(first),
                    first.chunkTitle(),
                    "SECTION",
                    chunkMetadata(first, parentContent, true, knowledgeBase.embeddingModelId())
            );
            store.saveChunk(parent);
            persistedParentIds.put(entry.getKey(), parentId);
        }

        for (KnowledgeChunkPreview preview : previews) {
            String parentKey = preview.parentChunkId() == null || preview.parentChunkId().isBlank()
                    ? "parent_" + preview.index()
                    : preview.parentChunkId();
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
            ).withStructure(
                    logicalChunkId(document.id(), "child_" + preview.index()),
                    persistedParentIds.get(parentKey),
                    normalizeGroupId(document.id(), preview.groupId()),
                    "CHILD",
                    sectionPath(preview),
                    preview.chunkTitle(),
                    preview.chunkType(),
                    chunkMetadata(preview, preview.embeddingContent(), false, knowledgeBase.embeddingModelId())
            ));
            saveEmbeddingIfConfigured(knowledgeBase, chunk);
        }
        com.mw.ai.agi.knowledge.chunking.ChunkingQualityReport quality =
                com.mw.ai.agi.knowledge.chunking.ChunkingQualityReport.from(previews);
        QUALITY_LOG.info(
                "event=chunking documentId={} parserVersion={} profileVersion={} childChunkCount={} parentChunkCount={} atomicBlockCount={} avgChunkTokens={} maxChunkTokens={}",
                document.id(),
                "structure-parser-v1",
                "chunk-profile-v1.1",
                quality.chunkCount(),
                parentGroups.size(),
                quality.atomicChunkCount(),
                quality.averageTokens(),
                quality.maxTokens()
        );
    }

    private String logicalChunkId(String documentId, String key) {
        return "logical_" + Integer.toUnsignedString((documentId + "|" + key).hashCode(), 36);
    }

    private String normalizeGroupId(String documentId, String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return null;
        }
        return "group_" + Integer.toUnsignedString((documentId + "|" + groupId).hashCode(), 36);
    }

    private String sectionPath(KnowledgeChunkPreview preview) {
        return preview.sectionPath().isEmpty() ? null : String.join(" > ", preview.sectionPath());
    }

    private String chunkMetadata(
            KnowledgeChunkPreview preview,
            String embeddingContent,
            boolean parent,
            String embeddingModelId
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        try {
            metadata.putAll(objectMapper.readValue(preview.metadataJson(), Map.class));
        } catch (Exception ignored) {
        }
        String effectiveEmbeddingContent = embeddingContent == null ? preview.content() : embeddingContent;
        metadata.put("embeddingContent", effectiveEmbeddingContent);
        metadata.put("embeddingContentSnapshot", effectiveEmbeddingContent);
        metadata.put("contentHash", sha256(preview.content()));
        metadata.put("embeddingContentHash", sha256(effectiveEmbeddingContent));
        metadata.put("embeddingModelId", embeddingModelId);
        metadata.put("parserVersion", "structure-parser-v1");
        metadata.put("profileVersion", "chunk-profile-v1.1");
        metadata.put("splitReason", preview.splitReason());
        metadata.put("atomic", preview.atomic());
        metadata.put("parent", parent);
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception ignored) {
            return preview.metadataJson();
        }
    }

    private String embeddingContent(KnowledgeChunk chunk) {
        Object value = parseMetadata(chunk).get("embeddingContent");
        return value == null || String.valueOf(value).isBlank() ? chunk.content() : String.valueOf(value);
    }

    private String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public int countChunksByDataset(String knowledgeBaseId, String datasetId) {
        return (int) store.listChunks(knowledgeBaseId).stream()
                .filter(this::isRetrievableChild)
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
                    .ifPresent(config -> provider(config).deleteDocument(
                            config, entity.getKnowledgeBaseId(), documentId
                    ));
            store.deleteChunkVectors(entity.getKnowledgeBaseId(), documentId);
            store.deleteChunks(entity.getKnowledgeBaseId(), documentId);
            document = document.withProcessingStatus("PROCESSING", null);
        }
        store.saveDocument(document);
        List<KnowledgeChunkPreview> previews = documentSplitter.splitDocument(
                document.name(),
                content,
                new KnowledgeSplitRequest(
                        knowledgeBase.splitterType(),
                        knowledgeBase.chunkSize(),
                        knowledgeBase.chunkOverlap(),
                        null,
                        knowledgeBase.embeddingModelId(),
                        knowledgeBase.semanticSimilarityThreshold()
                )
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

    private boolean isRetrievableChild(KnowledgeChunk chunk) {
        return chunk != null && !"PARENT".equalsIgnoreCase(chunk.chunkLevel());
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
        int chunkOverlap = intConfig(document.splitterConfig(), "chunkOverlap", 0);
        double semanticSimilarityThreshold = doubleConfig(
                document.splitterConfig(), "semanticSimilarityThreshold", 0.78
        );
        return new KnowledgeSplitRequest(
                defaultString(document.splitterType(), "FIXED_LENGTH"),
                chunkSize,
                chunkOverlap,
                separator,
                null,
                semanticSimilarityThreshold
        );
    }

    private String splitConfig(KnowledgeSplitRequest splitRequest) {
        String separator = splitRequest.separator() == null ? "" : splitRequest.separator()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "{\"chunkSize\":" + splitRequest.effectiveChunkSize()
                + ",\"chunkOverlap\":" + splitRequest.effectiveChunkOverlap()
                + ",\"separator\":\"" + separator + "\""
                + ",\"semanticSimilarityThreshold\":"
                + splitRequest.effectiveSemanticSimilarityThreshold()
                + "}";
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

    private double doubleConfig(String json, String key, double fallback) {
        String value = stringConfig(json, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
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
        java.util.regex.Matcher number = java.util.regex.Pattern.compile(
                "\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)"
        ).matcher(json);
        return number.find() ? number.group(1) : null;
    }

    private KnowledgeSplitRequest effectiveSplitRequest(
            KnowledgeBase knowledgeBase,
            KnowledgeSplitRequest request
    ) {
        KnowledgeSplitRequest source = request == null
                ? new KnowledgeSplitRequest(
                        knowledgeBase.splitterType(),
                        knowledgeBase.chunkSize(),
                        knowledgeBase.chunkOverlap(),
                        null
                )
                : request;
        String embeddingModelId = source.embeddingModelId() == null
                || source.embeddingModelId().isBlank()
                ? knowledgeBase.embeddingModelId()
                : source.embeddingModelId();
        return new KnowledgeSplitRequest(
                source.splitterType(),
                source.chunkSize(),
                source.chunkOverlap(),
                source.separator(),
                embeddingModelId,
                normalizeSemanticThreshold(
                        source.semanticSimilarityThreshold(),
                        knowledgeBase.semanticSimilarityThreshold()
                )
        );
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
                .filter(this::isRetrievableChild)
                .filter(chunk -> !vectorChunkIds.contains(chunk.id()))
                .toList();
        for (int start = 0; start < missingChunks.size(); start += EMBEDDING_BACKFILL_BATCH_SIZE) {
            List<KnowledgeChunk> batch = missingChunks.subList(start, Math.min(start + EMBEDDING_BACKFILL_BATCH_SIZE, missingChunks.size()));
            List<List<Double>> embeddings = embeddingClient.embedAll(
                    knowledgeBase.embeddingModelId(),
                    knowledgeBase.embeddingModelId(),
                    batch.stream().map(this::embeddingContent).toList()
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
                        .ifPresent(config -> provider(config).upsertChunk(config, chunk, vector));
                created++;
            }
        }
        return created;
    }

    private Optional<VectorStoreConfig> externalVectorStore(KnowledgeBase knowledgeBase) {
        if (knowledgeBase.vectorStoreConfigId() == null || knowledgeBase.vectorStoreConfigId().isBlank()) {
            return Optional.empty();
        }
        VectorStoreConfig config = vectorStoreConfigStore.findById(knowledgeBase.vectorStoreConfigId())
                .orElseThrow(() -> new IllegalStateException(
                        "Vector store config not found: " + knowledgeBase.vectorStoreConfigId()
                ));
        if (!config.enabled()) {
            throw new IllegalStateException("Vector store config is disabled: " + config.name());
        }
        if ("MEMORY".equalsIgnoreCase(config.storeType())) {
            return Optional.empty();
        }
        provider(config);
        return Optional.of(config);
    }

    private VectorStoreProvider provider(VectorStoreConfig config) {
        return vectorStoreProviders.require(config.storeType());
    }

    private void validateVectorStoreDimension(String configId, int vectorDimension) {
        if (configId == null || configId.isBlank()) {
            return;
        }
        VectorStoreConfig config = vectorStoreConfigStore.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Vector store config not found: " + configId));
        if (!"MEMORY".equalsIgnoreCase(config.storeType())
                && config.vectorDimension() != vectorDimension) {
            throw new IllegalArgumentException("Knowledge base vector dimension " + vectorDimension
                    + " does not match vector store dimension " + config.vectorDimension());
        }
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
        int keywordScore = keywordScore(chunk, terms);
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
        for (String term : normalized.split("[\\s,，。；;:：！？?、]+")) {
            if (!term.isBlank()) {
                terms.add(term);
                appendCjkTerms(term, terms);
                java.util.regex.Matcher alphanumeric =
                        java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_.-]*").matcher(term);
                while (alphanumeric.find()) {
                    terms.add(alphanumeric.group());
                }
            }
        }
        appendMixedTerms(normalized, terms);
        return terms;
    }

    private void appendMixedTerms(String text, Set<String> terms) {
        String compact = text.replaceAll("[^\\p{IsHan}a-z0-9]", "");
        for (int size = 2; size <= 3; size++) {
            for (int index = 0; index <= compact.length() - size; index++) {
                terms.add(compact.substring(index, index + size));
            }
        }
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
        for (int index = 0; index < segment.length() - 2; index++) {
            terms.add(segment.substring(index, index + 3));
        }
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private int score(String content, Set<String> terms) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String normalized = content.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        int score = 0;
        boolean matchedSignificantTerm = false;
        for (String term : terms) {
            if (term.isBlank() || !normalized.contains(term)) {
                continue;
            }
            if (term.length() >= 2) {
                matchedSignificantTerm = true;
                score += term.length() * term.length() * 10;
            }
        }
        return matchedSignificantTerm ? score : 0;
    }

    private int keywordScore(KnowledgeChunk chunk, Set<String> terms) {
        return score(chunk.content(), terms)
                + score(chunk.documentName(), terms) * 2
                + score(chunk.chunkTitle(), terms) * 3
                + score(chunk.sectionPath(), terms) * 2;
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

    private double normalizeSemanticThreshold(Double value, double fallback) {
        double threshold = value == null ? fallback : value;
        return Math.max(0, Math.min(1, threshold));
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
            List<KnowledgeChunkPreview> chunks,
            List<String> warnings
    ) {
        public UploadedDocumentPreview(String fileName, int characterCount, List<KnowledgeChunkPreview> chunks) {
            this(fileName, characterCount, chunks, List.of());
        }

        public UploadedDocumentPreview {
            chunks = chunks == null ? List.of() : List.copyOf(chunks);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
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
