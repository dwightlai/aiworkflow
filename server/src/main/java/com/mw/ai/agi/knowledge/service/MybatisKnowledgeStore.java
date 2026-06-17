package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.KnowledgeDocument;
import com.mw.ai.agi.knowledge.persistence.KnowledgeBaseEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeBaseMapper;
import com.mw.ai.agi.knowledge.persistence.KnowledgeChunkEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeChunkMapper;
import com.mw.ai.agi.knowledge.persistence.KnowledgeChunkVectorEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeChunkVectorMapper;
import com.mw.ai.agi.knowledge.persistence.KnowledgeDocumentEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeDocumentMapper;
import com.mw.ai.agi.persistence.JsonSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Optional;

public class MybatisKnowledgeStore implements KnowledgeStore {
    private static final TypeReference<List<Double>> DOUBLE_LIST = new TypeReference<>() {
    };

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeChunkVectorMapper chunkVectorMapper;
    private final JsonSupport jsonSupport;

    public MybatisKnowledgeStore(
            KnowledgeBaseMapper knowledgeBaseMapper,
            KnowledgeDocumentMapper documentMapper,
            KnowledgeChunkMapper chunkMapper,
            KnowledgeChunkVectorMapper chunkVectorMapper,
            JsonSupport jsonSupport
    ) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.chunkVectorMapper = chunkVectorMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public KnowledgeBase saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        KnowledgeBaseEntity entity = toEntity(knowledgeBase);
        if (knowledgeBaseMapper.selectById(knowledgeBase.id()) == null) {
            knowledgeBaseMapper.insert(entity);
        } else {
            knowledgeBaseMapper.updateById(entity);
        }
        return knowledgeBase;
    }

    @Override
    public Optional<KnowledgeBase> findKnowledgeBaseById(String id) {
        return Optional.ofNullable(knowledgeBaseMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<KnowledgeBase> listKnowledgeBases(String tenantId) {
        LambdaQueryWrapper<KnowledgeBaseEntity> wrapper = new LambdaQueryWrapper<KnowledgeBaseEntity>()
                .orderByAsc(KnowledgeBaseEntity::getCreatedAt);
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(KnowledgeBaseEntity::getTenantId, tenantId);
        }
        return knowledgeBaseMapper.selectList(wrapper)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteKnowledgeBase(String id) {
        deleteChunkVectors(id);
        deleteChunks(id);
        deleteDocuments(id);
        knowledgeBaseMapper.deleteById(id);
    }

    @Override
    public KnowledgeDocument saveDocument(KnowledgeDocument document) {
        KnowledgeDocumentEntity entity = toEntity(document);
        if (documentMapper.selectById(document.id()) == null) {
            documentMapper.insert(entity);
        } else {
            documentMapper.updateById(entity);
        }
        return document;
    }

    @Override
    public List<KnowledgeDocument> listDocuments(String knowledgeBaseId) {
        return documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByAsc(KnowledgeDocumentEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteDocuments(String knowledgeBaseId) {
        documentMapper.delete(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId));
    }

    @Override
    public void deleteDocument(String knowledgeBaseId, String documentId) {
        documentMapper.delete(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocumentEntity::getId, documentId));
    }

    @Override
    public KnowledgeChunk saveChunk(KnowledgeChunk chunk) {
        KnowledgeChunkEntity entity = toEntity(chunk);
        if (chunkMapper.selectById(chunk.id()) == null) {
            chunkMapper.insert(entity);
        } else {
            chunkMapper.updateById(entity);
        }
        return chunk;
    }

    @Override
    public List<KnowledgeChunk> listChunks(String knowledgeBaseId, String documentId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                        .eq(KnowledgeChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeChunkEntity::getDocumentId, documentId)
                        .orderByAsc(KnowledgeChunkEntity::getChunkIndex))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<KnowledgeChunk> listChunks(String knowledgeBaseId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                        .eq(KnowledgeChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByAsc(KnowledgeChunkEntity::getDocumentName)
                        .orderByAsc(KnowledgeChunkEntity::getChunkIndex))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteChunks(String knowledgeBaseId) {
        deleteChunkVectors(knowledgeBaseId);
        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getKnowledgeBaseId, knowledgeBaseId));
    }

    @Override
    public void deleteChunks(String knowledgeBaseId, String documentId) {
        deleteChunkVectors(knowledgeBaseId, documentId);
        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeChunkEntity::getDocumentId, documentId));
    }

    @Override
    public KnowledgeChunkVector saveChunkVector(KnowledgeChunkVector vector) {
        KnowledgeChunkVectorEntity entity = toEntity(vector);
        if (chunkVectorMapper.selectById(vector.chunkId()) == null) {
            chunkVectorMapper.insert(entity);
        } else {
            chunkVectorMapper.updateById(entity);
        }
        return vector;
    }

    @Override
    public List<KnowledgeChunkVector> listChunkVectors(String knowledgeBaseId) {
        return chunkVectorMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkVectorEntity>()
                        .eq(KnowledgeChunkVectorEntity::getKnowledgeBaseId, knowledgeBaseId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteChunkVectors(String knowledgeBaseId) {
        chunkVectorMapper.delete(new LambdaQueryWrapper<KnowledgeChunkVectorEntity>()
                .eq(KnowledgeChunkVectorEntity::getKnowledgeBaseId, knowledgeBaseId));
    }

    @Override
    public void deleteChunkVectors(String knowledgeBaseId, String documentId) {
        chunkVectorMapper.delete(new LambdaQueryWrapper<KnowledgeChunkVectorEntity>()
                .eq(KnowledgeChunkVectorEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeChunkVectorEntity::getDocumentId, documentId));
    }

    private KnowledgeBaseEntity toEntity(KnowledgeBase knowledgeBase) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(knowledgeBase.id());
        entity.setTenantId(knowledgeBase.tenantId());
        entity.setName(knowledgeBase.name());
        entity.setDescription(knowledgeBase.description());
        entity.setOwnerUnitId(knowledgeBase.ownerUnitId());
        entity.setEmbeddingModelId(knowledgeBase.embeddingModelId());
        entity.setVectorStoreConfigId(knowledgeBase.vectorStoreConfigId());
        entity.setVectorDimension(knowledgeBase.vectorDimension());
        entity.setSplitterType(knowledgeBase.splitterType());
        entity.setChunkSize(knowledgeBase.chunkSize());
        entity.setChunkOverlap(knowledgeBase.chunkOverlap());
        entity.setRetrievalMode(knowledgeBase.retrievalMode());
        entity.setTopK(knowledgeBase.topK());
        entity.setStatus(knowledgeBase.status());
        entity.setDocumentCount(knowledgeBase.documentCount());
        entity.setChunkCount(knowledgeBase.chunkCount());
        entity.setCreatedBy(knowledgeBase.createdBy());
        entity.setUpdatedBy(knowledgeBase.updatedBy());
        entity.setCreatedAt(knowledgeBase.createdAt());
        entity.setUpdatedAt(knowledgeBase.updatedAt());
        entity.setKbType(knowledgeBase.kbType());
        entity.setBizScope(knowledgeBase.bizScope());
        entity.setDatasetMode(knowledgeBase.datasetMode());
        entity.setDefaultDatasetId(knowledgeBase.defaultDatasetId());
        entity.setDatasetCount(knowledgeBase.datasetCount());
        entity.setMetadataJson(knowledgeBase.metadataJson());
        return entity;
    }

    private KnowledgeBase toDomain(KnowledgeBaseEntity entity) {
        return new KnowledgeBase(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwnerUnitId(),
                entity.getEmbeddingModelId(),
                entity.getVectorStoreConfigId(),
                entity.getVectorDimension(),
                entity.getSplitterType(),
                entity.getChunkSize(),
                entity.getChunkOverlap(),
                entity.getRetrievalMode(),
                entity.getTopK(),
                entity.getStatus(),
                entity.getDocumentCount(),
                entity.getChunkCount(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                defaultString(entity.getKbType(), "NORMAL"),
                entity.getBizScope(),
                defaultString(entity.getDatasetMode(), "SINGLE"),
                entity.getDefaultDatasetId(),
                entity.getDatasetCount() == null ? 0 : entity.getDatasetCount(),
                entity.getMetadataJson()
        );
    }

    private KnowledgeDocumentEntity toEntity(KnowledgeDocument document) {
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setId(document.id());
        entity.setKnowledgeBaseId(document.knowledgeBaseId());
        entity.setName(document.name());
        entity.setChunkCount(document.chunkCount());
        entity.setCreatedAt(document.createdAt());
        entity.setDatasetType(document.datasetType());
        entity.setProcessingStatus(document.processingStatus());
        entity.setTags(document.tags());
        entity.setCategory(document.category());
        entity.setSource(document.source());
        entity.setRowCount(document.rowCount());
        entity.setParserType(document.parserType());
        entity.setSplitterType(document.splitterType());
        entity.setSplitterConfig(document.splitterConfig());
        entity.setRawContent(document.rawContent());
        entity.setErrorMessage(document.errorMessage());
        entity.setStoragePath(document.storagePath());
        entity.setDatasetId(document.datasetId());
        entity.setSourceIndexId(document.sourceIndexId());
        entity.setTopicId(document.topicId());
        entity.setDocType(document.docType());
        entity.setSourceSystem(document.sourceSystem());
        entity.setSourceType(document.sourceType());
        entity.setSourceRefId(document.sourceRefId());
        entity.setMaterialSourceType(document.materialSourceType());
        entity.setMaterialType(document.materialType());
        entity.setSourceArchiveFileId(document.sourceArchiveFileId());
        entity.setSourceVersion(document.sourceVersion());
        entity.setTitleSnapshot(document.titleSnapshot());
        entity.setMetadataJson(document.metadataJson());
        entity.setSummaryText(document.summaryText());
        entity.setSecurityLevel(document.securityLevel());
        entity.setOwnerUnitId(document.ownerUnitId());
        entity.setLastIndexTime(document.lastIndexTime());
        return entity;
    }

    private KnowledgeDocument toDomain(KnowledgeDocumentEntity entity) {
        return new KnowledgeDocument(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getName(),
                entity.getChunkCount() == null ? 0 : entity.getChunkCount(),
                entity.getCreatedAt(),
                defaultString(entity.getDatasetType(), "TEXT_DOCUMENT"),
                defaultString(entity.getProcessingStatus(), "READY"),
                entity.getTags(),
                entity.getCategory(),
                entity.getSource(),
                entity.getRowCount() == null ? 0 : entity.getRowCount(),
                defaultString(entity.getParserType(), "TEXT"),
                defaultString(entity.getSplitterType(), "FIXED_LENGTH"),
                defaultString(entity.getSplitterConfig(), "{}"),
                entity.getRawContent(),
                entity.getErrorMessage(),
                entity.getStoragePath(),
                entity.getDatasetId(),
                entity.getSourceIndexId(),
                entity.getTopicId(),
                entity.getDocType(),
                entity.getSourceSystem(),
                entity.getSourceType(),
                entity.getSourceRefId(),
                entity.getMaterialSourceType(),
                entity.getMaterialType(),
                entity.getSourceArchiveFileId(),
                entity.getSourceVersion(),
                entity.getTitleSnapshot(),
                entity.getMetadataJson(),
                entity.getSummaryText(),
                entity.getSecurityLevel(),
                entity.getOwnerUnitId(),
                entity.getLastIndexTime()
        );
    }

    private KnowledgeChunkEntity toEntity(KnowledgeChunk chunk) {
        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        entity.setId(chunk.id());
        entity.setKnowledgeBaseId(chunk.knowledgeBaseId());
        entity.setDocumentId(chunk.documentId());
        entity.setDocumentName(chunk.documentName());
        entity.setContent(chunk.content());
        entity.setChunkIndex(chunk.index());
        entity.setEnabled(chunk.enabled());
        entity.setTokenEstimate(chunk.tokenEstimate());
        entity.setDatasetId(chunk.datasetId());
        entity.setSourceIndexId(chunk.sourceIndexId());
        entity.setTopicId(chunk.topicId());
        entity.setChunkTitle(chunk.chunkTitle());
        entity.setChunkType(chunk.chunkType());
        entity.setSourceSystem(chunk.sourceSystem());
        entity.setSourceType(chunk.sourceType());
        entity.setSourceRefId(chunk.sourceRefId());
        entity.setMaterialSourceType(chunk.materialSourceType());
        entity.setMaterialType(chunk.materialType());
        entity.setSourceArchiveFileId(chunk.sourceArchiveFileId());
        entity.setSourcePage(chunk.sourcePage());
        entity.setSourcePosition(chunk.sourcePosition());
        entity.setCitationText(chunk.citationText());
        entity.setMetadataJson(chunk.metadataJson());
        entity.setSecurityLevel(chunk.securityLevel());
        return entity;
    }

    private KnowledgeChunk toDomain(KnowledgeChunkEntity entity) {
        return new KnowledgeChunk(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getDocumentId(),
                entity.getDocumentName(),
                entity.getContent(),
                entity.getChunkIndex(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getTokenEstimate(),
                entity.getDatasetId(),
                entity.getSourceIndexId(),
                entity.getTopicId(),
                entity.getChunkTitle(),
                entity.getChunkType(),
                entity.getSourceSystem(),
                entity.getSourceType(),
                entity.getSourceRefId(),
                entity.getMaterialSourceType(),
                entity.getMaterialType(),
                entity.getSourceArchiveFileId(),
                entity.getSourcePage(),
                entity.getSourcePosition(),
                entity.getCitationText(),
                entity.getMetadataJson(),
                entity.getSecurityLevel()
        );
    }

    private KnowledgeChunkVectorEntity toEntity(KnowledgeChunkVector vector) {
        KnowledgeChunkVectorEntity entity = new KnowledgeChunkVectorEntity();
        entity.setChunkId(vector.chunkId());
        entity.setKnowledgeBaseId(vector.knowledgeBaseId());
        entity.setDocumentId(vector.documentId());
        entity.setEmbeddingModelId(vector.embeddingModelId());
        entity.setEmbedding(jsonSupport.write(vector.embedding()));
        entity.setCreatedAt(vector.createdAt());
        return entity;
    }

    private KnowledgeChunkVector toDomain(KnowledgeChunkVectorEntity entity) {
        return new KnowledgeChunkVector(
                entity.getChunkId(),
                entity.getKnowledgeBaseId(),
                entity.getDocumentId(),
                entity.getEmbeddingModelId(),
                jsonSupport.read(entity.getEmbedding(), DOUBLE_LIST),
                entity.getCreatedAt()
        );
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
