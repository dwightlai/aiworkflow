package com.mw.ai.agi.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeDataset;
import com.mw.ai.agi.knowledge.domain.KnowledgeSourceIndex;
import com.mw.ai.agi.knowledge.persistence.KnowledgeSourceIndexEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeSourceIndexMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeSourceSyncService {
    private final KnowledgeSourceIndexMapper sourceIndexMapper;
    private final KnowledgeDatasetService datasetService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;

    public KnowledgeSourceSyncService(
            KnowledgeSourceIndexMapper sourceIndexMapper,
            KnowledgeDatasetService datasetService,
            KnowledgeBaseService knowledgeBaseService,
            ObjectMapper objectMapper
    ) {
        this.sourceIndexMapper = sourceIndexMapper;
        this.datasetService = datasetService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.objectMapper = objectMapper;
    }

    public SyncResult syncArchiveTopic(String knowledgeBaseId, String topicId, ArchiveTopicSyncCommand command) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        String effectiveTopicId = requireTopicId(topicId);
        String topicTitle = command == null || command.topicTitle() == null || command.topicTitle().isBlank()
                ? effectiveTopicId
                : command.topicTitle().trim();
        KnowledgeDataset dataset = datasetService.findOrCreateTopicDataset(knowledgeBase, effectiveTopicId, topicTitle);
        List<Map<String, Object>> materials = normalizeMaterials(command == null ? List.of() : command.materials());
        if (materials.isEmpty()) {
            throw new IllegalArgumentException("同步资料列表不能为空");
        }
        boolean reindexChanged = command == null || command.reindexChanged() == null || command.reindexChanged();
        return syncMaterials(knowledgeBase, dataset, effectiveTopicId, materials, reindexChanged);
    }

    public KnowledgeSourceIndex addManualTextSource(String datasetId, String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("资料标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("资料内容不能为空");
        }
        KnowledgeDataset dataset = datasetService.getRequired(datasetId);
        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(dataset.knowledgeBaseId());
        Instant now = Instant.now();
        String sourceRefId = "manual_" + UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> material = new LinkedHashMap<>();
        material.put("sourceRefId", sourceRefId);
        material.put("id", sourceRefId);
        material.put("title", title.trim());
        material.put("contentText", content.trim());
        material.put("summary", content.trim());
        material.put("materialSourceType", "MANUAL_TEXT");
        material.put("materialType", "TOPIC_NOTE");
        KnowledgeSourceIndex created = createSourceIndex(
                knowledgeBase,
                dataset,
                dataset.topicId(),
                material,
                "1",
                now,
                "KNOWLEDGE_SYSTEM",
                "MANUAL_TEXT",
                "TOPIC_NOTE",
                null
        );
        knowledgeBaseService.indexSourceMaterial(created.id());
        refreshDatasetStats(dataset, countSources(dataset.id()), 0, now);
        updateKnowledgeBaseDatasetCount(knowledgeBase);
        return getRequired(created.id());
    }

    public List<KnowledgeSourceIndex> listByDataset(String datasetId) {
        return sourceIndexMapper.selectList(new LambdaQueryWrapper<KnowledgeSourceIndexEntity>()
                        .eq(KnowledgeSourceIndexEntity::getDatasetId, datasetId)
                        .orderByDesc(KnowledgeSourceIndexEntity::getUpdatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    public KnowledgeSourceIndex getRequired(String sourceIndexId) {
        KnowledgeSourceIndexEntity entity = sourceIndexMapper.selectById(sourceIndexId);
        if (entity == null) {
            throw new IllegalArgumentException("Knowledge source index not found: " + sourceIndexId);
        }
        return toDomain(entity);
    }

    public KnowledgeSourceIndex save(KnowledgeSourceIndex sourceIndex) {
        KnowledgeSourceIndexEntity entity = toEntity(sourceIndex);
        if (sourceIndexMapper.selectById(sourceIndex.id()) == null) {
            sourceIndexMapper.insert(entity);
        } else {
            sourceIndexMapper.updateById(entity);
        }
        return sourceIndex;
    }

    private SyncResult syncMaterials(
            KnowledgeBase knowledgeBase,
            KnowledgeDataset dataset,
            String topicId,
            List<Map<String, Object>> materials,
            boolean reindexChanged
    ) {
        Instant now = Instant.now();
        int newCount = 0;
        int changedCount = 0;
        int unchangedCount = 0;
        int failedCount = 0;
        for (Map<String, Object> material : materials) {
            String sourceRefId = materialRefId(material);
            String version = materialVersion(material);
            KnowledgeSourceIndex existing = findBySourceRef(dataset.id(), sourceRefId);
            if (existing == null) {
                KnowledgeSourceIndex created = createSourceIndex(knowledgeBase, dataset, topicId, material, version, now);
                try {
                    knowledgeBaseService.indexSourceMaterial(created.id());
                    newCount++;
                } catch (RuntimeException exception) {
                    failedCount++;
                    markFailed(created.id(), exception.getMessage());
                }
                continue;
            }
            if (!version.equals(existing.sourceVersion())) {
                KnowledgeSourceIndex updated = updateSourceIndex(existing, material, version, now);
                if (reindexChanged) {
                    try {
                        knowledgeBaseService.indexSourceMaterial(updated.id());
                        changedCount++;
                    } catch (RuntimeException exception) {
                        failedCount++;
                        markFailed(updated.id(), exception.getMessage());
                    }
                } else {
                    markStale(updated.id());
                    changedCount++;
                }
                continue;
            }
            unchangedCount++;
        }
        refreshDatasetStats(dataset, materials.size(), failedCount, now);
        updateKnowledgeBaseDatasetCount(knowledgeBase);
        return new SyncResult(dataset.id(), materials.size(), newCount, changedCount, unchangedCount, failedCount);
    }

    private KnowledgeSourceIndex findBySourceRef(String datasetId, String sourceRefId) {
        KnowledgeSourceIndexEntity entity = sourceIndexMapper.selectOne(new LambdaQueryWrapper<KnowledgeSourceIndexEntity>()
                .eq(KnowledgeSourceIndexEntity::getDatasetId, datasetId)
                .eq(KnowledgeSourceIndexEntity::getSourceRefId, sourceRefId)
                .last("LIMIT 1"));
        return entity == null ? null : toDomain(entity);
    }

    private KnowledgeSourceIndex createSourceIndex(
            KnowledgeBase knowledgeBase,
            KnowledgeDataset dataset,
            String topicId,
            Map<String, Object> material,
            String version,
            Instant now
    ) {
        return createSourceIndex(
                knowledgeBase,
                dataset,
                topicId,
                material,
                version,
                now,
                String.valueOf(material.getOrDefault("sourceSystem", "DIGITAL_ARCHIVE")),
                String.valueOf(material.getOrDefault("materialSourceType", "ARCHIVE_FILE")),
                String.valueOf(material.getOrDefault("materialType", "PROJECT_DOCUMENT")),
                material.get("sourceArchiveFileId") == null ? null : String.valueOf(material.get("sourceArchiveFileId"))
        );
    }

    private KnowledgeSourceIndex createSourceIndex(
            KnowledgeBase knowledgeBase,
            KnowledgeDataset dataset,
            String topicId,
            Map<String, Object> material,
            String version,
            Instant now,
            String sourceSystem,
            String materialSourceType,
            String materialType,
            String sourceArchiveFileId
    ) {
        String sourceRefId = materialRefId(material);
        String title = String.valueOf(material.getOrDefault("title", sourceRefId));
        KnowledgeSourceIndex sourceIndex = new KnowledgeSourceIndex(
                "src_" + UUID.randomUUID().toString().replace("-", ""),
                knowledgeBase.tenantId(),
                knowledgeBase.id(),
                dataset.id(),
                topicId,
                sourceSystem,
                String.valueOf(material.getOrDefault("sourceType", "ARCHIVE_TOPIC_MATERIAL")),
                sourceRefId,
                materialSourceType,
                materialType,
                sourceArchiveFileId,
                title,
                version,
                null,
                null,
                writeJson(material),
                null,
                "NOT_INDEXED",
                now,
                null,
                null,
                now,
                now
        );
        return save(sourceIndex);
    }

    private KnowledgeSourceIndex updateSourceIndex(
            KnowledgeSourceIndex existing,
            Map<String, Object> material,
            String version,
            Instant now
    ) {
        return save(new KnowledgeSourceIndex(
                existing.id(),
                existing.tenantId(),
                existing.knowledgeBaseId(),
                existing.datasetId(),
                existing.topicId(),
                existing.sourceSystem(),
                existing.sourceType(),
                existing.sourceRefId(),
                stringOrDefault(material.get("materialSourceType"), existing.materialSourceType()),
                stringOrDefault(material.get("materialType"), existing.materialType()),
                material.get("sourceArchiveFileId") == null
                        ? existing.sourceArchiveFileId()
                        : String.valueOf(material.get("sourceArchiveFileId")),
                String.valueOf(material.getOrDefault("title", existing.sourceTitleSnapshot())),
                version,
                existing.sourceUrl(),
                existing.storagePath(),
                writeJson(material),
                existing.documentId(),
                "STALE",
                now,
                existing.lastIndexTime(),
                null,
                existing.createdAt(),
                now
        ));
    }

    private void markFailed(String sourceIndexId, String message) {
        KnowledgeSourceIndex existing = getRequired(sourceIndexId);
        save(new KnowledgeSourceIndex(
                existing.id(), existing.tenantId(), existing.knowledgeBaseId(), existing.datasetId(), existing.topicId(),
                existing.sourceSystem(), existing.sourceType(), existing.sourceRefId(), existing.materialSourceType(),
                existing.materialType(), existing.sourceArchiveFileId(), existing.sourceTitleSnapshot(), existing.sourceVersion(),
                existing.sourceUrl(), existing.storagePath(), existing.metadataSnapshot(), existing.documentId(),
                "FAILED", existing.lastSyncTime(), existing.lastIndexTime(), message, existing.createdAt(), Instant.now()
        ));
    }

    private void markStale(String sourceIndexId) {
        KnowledgeSourceIndex existing = getRequired(sourceIndexId);
        save(new KnowledgeSourceIndex(
                existing.id(), existing.tenantId(), existing.knowledgeBaseId(), existing.datasetId(), existing.topicId(),
                existing.sourceSystem(), existing.sourceType(), existing.sourceRefId(), existing.materialSourceType(),
                existing.materialType(), existing.sourceArchiveFileId(), existing.sourceTitleSnapshot(), existing.sourceVersion(),
                existing.sourceUrl(), existing.storagePath(), existing.metadataSnapshot(), existing.documentId(),
                "STALE", existing.lastSyncTime(), existing.lastIndexTime(), existing.errorMessage(),
                existing.createdAt(), Instant.now()
        ));
    }

    private List<Map<String, Object>> normalizeMaterials(List<SourceMaterialPayload> materials) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (SourceMaterialPayload material : materials) {
            if (material == null || material.sourceRefId() == null || material.sourceRefId().isBlank()) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sourceRefId", material.sourceRefId().trim());
            map.put("id", material.sourceRefId().trim());
            map.put("sourceVersion", material.sourceVersion());
            map.put("title", material.title());
            map.put("materialSourceType", material.materialSourceType());
            map.put("materialType", material.materialType());
            map.put("sourceArchiveFileId", material.sourceArchiveFileId());
            map.put("contentText", material.contentText());
            map.put("summary", material.contentText());
            if (material.metadata() != null) {
                map.putAll(material.metadata());
            }
            normalized.add(map);
        }
        return normalized;
    }

    private String requireTopicId(String topicId) {
        if (topicId == null || topicId.isBlank()) {
            throw new IllegalArgumentException("topicId 不能为空");
        }
        return topicId.trim();
    }

    private String materialRefId(Map<String, Object> material) {
        Object ref = material.get("sourceRefId");
        if (ref != null && !String.valueOf(ref).isBlank()) {
            return String.valueOf(ref).trim();
        }
        return String.valueOf(material.get("id")).trim();
    }

    private String materialVersion(Map<String, Object> material) {
        Object version = material.get("sourceVersion");
        if (version != null && !String.valueOf(version).isBlank()) {
            return String.valueOf(version).trim();
        }
        return String.valueOf(material.getOrDefault("formationDate", "1"));
    }

    private String stringOrDefault(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }

    private int countDocuments(String datasetId) {
        return Math.toIntExact(sourceIndexMapper.selectCount(new LambdaQueryWrapper<KnowledgeSourceIndexEntity>()
                .eq(KnowledgeSourceIndexEntity::getDatasetId, datasetId)
                .isNotNull(KnowledgeSourceIndexEntity::getDocumentId)));
    }

    private int countSources(String datasetId) {
        return Math.toIntExact(sourceIndexMapper.selectCount(new LambdaQueryWrapper<KnowledgeSourceIndexEntity>()
                .eq(KnowledgeSourceIndexEntity::getDatasetId, datasetId)));
    }

    private int countChunks(String knowledgeBaseId, String datasetId) {
        return knowledgeBaseService.countChunksByDataset(knowledgeBaseId, datasetId);
    }

    private void refreshDatasetStats(KnowledgeDataset dataset, int sourceCount, int failedCount, Instant now) {
        datasetService.refreshStats(
                dataset.id(),
                countDocuments(dataset.id()),
                countChunks(dataset.knowledgeBaseId(), dataset.id()),
                sourceCount,
                failedCount > 0 ? "FAILED" : "READY"
        );
        KnowledgeDataset current = datasetService.getRequired(dataset.id());
        datasetService.save(new KnowledgeDataset(
                current.id(), current.tenantId(), current.knowledgeBaseId(), current.name(), current.code(),
                current.description(), current.datasetType(), current.bizType(), current.bizId(), current.topicId(),
                current.topicTitle(), current.securityLevel(), current.retentionPeriod(), current.ownerUnitId(),
                current.sourceSystem(), current.sourceVersion(), current.documentCount(), current.chunkCount(),
                sourceCount, failedCount > 0 ? "FAILED" : "READY", now, now, current.metadataJson(), current.status(),
                current.createdBy(), current.updatedBy(), current.createdAt(), now
        ));
    }

    private void updateKnowledgeBaseDatasetCount(KnowledgeBase knowledgeBase) {
        int count = datasetService.listByKnowledgeBase(knowledgeBase.id()).size();
        knowledgeBaseService.updateDatasetCount(knowledgeBase.id(), count);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private KnowledgeSourceIndexEntity toEntity(KnowledgeSourceIndex sourceIndex) {
        KnowledgeSourceIndexEntity entity = new KnowledgeSourceIndexEntity();
        entity.setId(sourceIndex.id());
        entity.setTenantId(sourceIndex.tenantId());
        entity.setKnowledgeBaseId(sourceIndex.knowledgeBaseId());
        entity.setDatasetId(sourceIndex.datasetId());
        entity.setTopicId(sourceIndex.topicId());
        entity.setSourceSystem(sourceIndex.sourceSystem());
        entity.setSourceType(sourceIndex.sourceType());
        entity.setSourceRefId(sourceIndex.sourceRefId());
        entity.setMaterialSourceType(sourceIndex.materialSourceType());
        entity.setMaterialType(sourceIndex.materialType());
        entity.setSourceArchiveFileId(sourceIndex.sourceArchiveFileId());
        entity.setSourceTitleSnapshot(sourceIndex.sourceTitleSnapshot());
        entity.setSourceVersion(sourceIndex.sourceVersion());
        entity.setSourceUrl(sourceIndex.sourceUrl());
        entity.setStoragePath(sourceIndex.storagePath());
        entity.setMetadataSnapshot(sourceIndex.metadataSnapshot());
        entity.setDocumentId(sourceIndex.documentId());
        entity.setIndexStatus(sourceIndex.indexStatus());
        entity.setLastSyncTime(sourceIndex.lastSyncTime());
        entity.setLastIndexTime(sourceIndex.lastIndexTime());
        entity.setErrorMessage(sourceIndex.errorMessage());
        entity.setCreatedAt(sourceIndex.createdAt());
        entity.setUpdatedAt(sourceIndex.updatedAt());
        return entity;
    }

    private KnowledgeSourceIndex toDomain(KnowledgeSourceIndexEntity entity) {
        return new KnowledgeSourceIndex(
                entity.getId(),
                entity.getTenantId(),
                entity.getKnowledgeBaseId(),
                entity.getDatasetId(),
                entity.getTopicId(),
                entity.getSourceSystem(),
                entity.getSourceType(),
                entity.getSourceRefId(),
                entity.getMaterialSourceType(),
                entity.getMaterialType(),
                entity.getSourceArchiveFileId(),
                entity.getSourceTitleSnapshot(),
                entity.getSourceVersion(),
                entity.getSourceUrl(),
                entity.getStoragePath(),
                entity.getMetadataSnapshot(),
                entity.getDocumentId(),
                entity.getIndexStatus(),
                entity.getLastSyncTime(),
                entity.getLastIndexTime(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public record ArchiveTopicSyncCommand(
            String topicTitle,
            Boolean reindexChanged,
            List<SourceMaterialPayload> materials
    ) {
    }

    public record SourceMaterialPayload(
            String sourceRefId,
            String sourceVersion,
            String title,
            String materialSourceType,
            String materialType,
            String sourceArchiveFileId,
            String contentText,
            Map<String, Object> metadata
    ) {
    }

    public record SyncResult(
            String datasetId,
            int sourceCount,
            int newCount,
            int changedCount,
            int unchangedCount,
            int failedCount
    ) {
    }
}
