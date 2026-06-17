package com.mw.ai.agi.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeDataset;
import com.mw.ai.agi.knowledge.persistence.KnowledgeDatasetEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeDatasetMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeDatasetService {
    private final KnowledgeDatasetMapper datasetMapper;

    public KnowledgeDatasetService(KnowledgeDatasetMapper datasetMapper) {
        this.datasetMapper = datasetMapper;
    }

    public KnowledgeDataset createManualDataset(
            KnowledgeBase knowledgeBase,
            String name,
            String code,
            String description,
            String topicId,
            String topicTitle
    ) {
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        String effectiveTopicId = blankToNull(topicId);
        KnowledgeDataset dataset = save(new KnowledgeDataset(
                "ds_" + UUID.randomUUID().toString().replace("-", ""),
                resolveTenantId(knowledgeBase),
                knowledgeBase.id(),
                name,
                blankToNull(code),
                blankToNull(description),
                effectiveTopicId == null ? "MANUAL" : "ARCHIVE_TOPIC",
                effectiveTopicId == null ? "MANUAL" : "ARCHIVE_TOPIC",
                effectiveTopicId,
                effectiveTopicId,
                blankToNull(topicTitle),
                null,
                null,
                knowledgeBase.ownerUnitId(),
                "KNOWLEDGE_SYSTEM",
                null,
                0,
                0,
                0,
                "NOT_INDEXED",
                null,
                null,
                null,
                "ENABLED",
                operator,
                operator,
                now,
                now
        ));
        return dataset;
    }

    public List<KnowledgeDataset> listManageableByKnowledgeBase(String knowledgeBaseId) {
        return listByKnowledgeBase(knowledgeBaseId).stream()
                .filter(dataset -> !"DEFAULT".equalsIgnoreCase(dataset.datasetType()))
                .toList();
    }

    public List<KnowledgeDataset> listForUi(KnowledgeBase knowledgeBase) {
        List<KnowledgeDataset> datasets = listByKnowledgeBase(knowledgeBase.id());
        if (!datasets.isEmpty()) {
            return datasets;
        }
        if (knowledgeBase.defaultDatasetId() != null && !knowledgeBase.defaultDatasetId().isBlank()) {
            KnowledgeDatasetEntity entity = datasetMapper.selectById(knowledgeBase.defaultDatasetId());
            if (entity != null) {
                return List.of(toDomain(entity));
            }
        }
        return datasets;
    }

    public boolean isDefaultDataset(KnowledgeDataset dataset) {
        return dataset != null && "DEFAULT".equalsIgnoreCase(dataset.datasetType());
    }

    public void assertMutableDataset(String datasetId) {
        if (isDefaultDataset(getRequired(datasetId))) {
            throw new IllegalArgumentException("默认分类不可编辑或删除");
        }
    }

    public KnowledgeDataset updateDataset(String datasetId, String name, String description) {
        assertMutableDataset(datasetId);
        KnowledgeDataset current = getRequired(datasetId);
        return save(new KnowledgeDataset(
                current.id(),
                current.tenantId(),
                current.knowledgeBaseId(),
                blankToNull(name) == null ? current.name() : name.trim(),
                current.code(),
                blankToNull(description),
                current.datasetType(),
                current.bizType(),
                current.bizId(),
                current.topicId(),
                current.topicTitle(),
                current.securityLevel(),
                current.retentionPeriod(),
                current.ownerUnitId(),
                current.sourceSystem(),
                current.sourceVersion(),
                current.documentCount(),
                current.chunkCount(),
                current.sourceCount(),
                current.indexStatus(),
                current.lastSyncTime(),
                current.lastIndexTime(),
                current.metadataJson(),
                current.status(),
                current.createdBy(),
                OperatorContext.currentUserId(),
                current.createdAt(),
                Instant.now()
        ));
    }

    public void deleteDataset(String datasetId) {
        assertMutableDataset(datasetId);
        KnowledgeDataset dataset = getRequired(datasetId);
        if (dataset.documentCount() > 0 || dataset.sourceCount() > 0) {
            throw new IllegalArgumentException("分类下仍有资料，无法删除");
        }
        datasetMapper.deleteById(datasetId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public KnowledgeDataset createDefaultDataset(KnowledgeBase knowledgeBase) {
        return save(new KnowledgeDataset(
                "ds_default_" + knowledgeBase.id(),
                resolveTenantId(knowledgeBase),
                knowledgeBase.id(),
                "默认分类",
                "default",
                null,
                "DEFAULT",
                null,
                null,
                null,
                null,
                null,
                null,
                knowledgeBase.ownerUnitId(),
                "KNOWLEDGE_SYSTEM",
                null,
                0,
                0,
                0,
                "READY",
                null,
                null,
                null,
                "ENABLED",
                OperatorContext.currentUserId(),
                OperatorContext.currentUserId(),
                Instant.now(),
                Instant.now()
        ));
    }

    public KnowledgeDataset findOrCreateTopicDataset(KnowledgeBase knowledgeBase, String topicId, String topicTitle) {
        KnowledgeDataset existing = findByTopic(knowledgeBase.id(), topicId);
        if (existing != null) {
            return existing;
        }
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        return save(new KnowledgeDataset(
                "ds_topic_" + UUID.randomUUID().toString().replace("-", ""),
                resolveTenantId(knowledgeBase),
                knowledgeBase.id(),
                topicTitle == null || topicTitle.isBlank() ? topicId : topicTitle,
                topicId,
                null,
                "ARCHIVE_TOPIC",
                "ARCHIVE_TOPIC",
                topicId,
                topicId,
                topicTitle,
                null,
                null,
                knowledgeBase.ownerUnitId(),
                "DIGITAL_ARCHIVE",
                null,
                0,
                0,
                0,
                "NOT_INDEXED",
                null,
                null,
                null,
                "ENABLED",
                operator,
                operator,
                now,
                now
        ));
    }

    public List<KnowledgeDataset> listByKnowledgeBase(String knowledgeBaseId) {
        return datasetMapper.selectList(new LambdaQueryWrapper<KnowledgeDatasetEntity>()
                        .eq(KnowledgeDatasetEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(KnowledgeDatasetEntity::getUpdatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    public KnowledgeDataset getRequired(String datasetId) {
        KnowledgeDatasetEntity entity = datasetMapper.selectById(datasetId);
        if (entity == null) {
            throw new IllegalArgumentException("Knowledge dataset not found: " + datasetId);
        }
        return toDomain(entity);
    }

    public KnowledgeDataset findByTopic(String knowledgeBaseId, String topicId) {
        KnowledgeDatasetEntity entity = datasetMapper.selectOne(new LambdaQueryWrapper<KnowledgeDatasetEntity>()
                .eq(KnowledgeDatasetEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDatasetEntity::getTopicId, topicId)
                .last("LIMIT 1"));
        return entity == null ? null : toDomain(entity);
    }

    public KnowledgeDataset save(KnowledgeDataset dataset) {
        KnowledgeDatasetEntity entity = toEntity(dataset);
        if (datasetMapper.selectById(dataset.id()) == null) {
            datasetMapper.insert(entity);
        } else {
            datasetMapper.updateById(entity);
        }
        return dataset;
    }

    public void refreshStats(String datasetId, int documentCount, int chunkCount, int sourceCount, String indexStatus) {
        KnowledgeDatasetEntity entity = datasetMapper.selectById(datasetId);
        if (entity == null) {
            return;
        }
        entity.setDocumentCount(documentCount);
        entity.setChunkCount(chunkCount);
        entity.setSourceCount(sourceCount);
        entity.setIndexStatus(indexStatus);
        entity.setLastIndexTime(Instant.now());
        entity.setUpdatedAt(Instant.now());
        datasetMapper.updateById(entity);
    }

    private KnowledgeDatasetEntity toEntity(KnowledgeDataset dataset) {
        KnowledgeDatasetEntity entity = new KnowledgeDatasetEntity();
        entity.setId(dataset.id());
        entity.setTenantId(dataset.tenantId());
        entity.setKnowledgeBaseId(dataset.knowledgeBaseId());
        entity.setName(dataset.name());
        entity.setCode(dataset.code());
        entity.setDescription(dataset.description());
        entity.setDatasetType(dataset.datasetType());
        entity.setBizType(dataset.bizType());
        entity.setBizId(dataset.bizId());
        entity.setTopicId(dataset.topicId());
        entity.setTopicTitle(dataset.topicTitle());
        entity.setSecurityLevel(dataset.securityLevel());
        entity.setRetentionPeriod(dataset.retentionPeriod());
        entity.setOwnerUnitId(dataset.ownerUnitId());
        entity.setSourceSystem(dataset.sourceSystem());
        entity.setSourceVersion(dataset.sourceVersion());
        entity.setDocumentCount(dataset.documentCount());
        entity.setChunkCount(dataset.chunkCount());
        entity.setSourceCount(dataset.sourceCount());
        entity.setIndexStatus(dataset.indexStatus());
        entity.setLastSyncTime(dataset.lastSyncTime());
        entity.setLastIndexTime(dataset.lastIndexTime());
        entity.setMetadataJson(dataset.metadataJson());
        entity.setStatus(dataset.status());
        entity.setCreatedBy(dataset.createdBy());
        entity.setUpdatedBy(dataset.updatedBy());
        entity.setCreatedAt(dataset.createdAt());
        entity.setUpdatedAt(dataset.updatedAt());
        return entity;
    }

    private KnowledgeDataset toDomain(KnowledgeDatasetEntity entity) {
        return new KnowledgeDataset(
                entity.getId(),
                entity.getTenantId(),
                entity.getKnowledgeBaseId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getDatasetType(),
                entity.getBizType(),
                entity.getBizId(),
                entity.getTopicId(),
                entity.getTopicTitle(),
                entity.getSecurityLevel(),
                entity.getRetentionPeriod(),
                entity.getOwnerUnitId(),
                entity.getSourceSystem(),
                entity.getSourceVersion(),
                entity.getDocumentCount() == null ? 0 : entity.getDocumentCount(),
                entity.getChunkCount() == null ? 0 : entity.getChunkCount(),
                entity.getSourceCount() == null ? 0 : entity.getSourceCount(),
                entity.getIndexStatus(),
                entity.getLastSyncTime(),
                entity.getLastIndexTime(),
                entity.getMetadataJson(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String resolveTenantId(KnowledgeBase knowledgeBase) {
        String tenantId = knowledgeBase.tenantId();
        return tenantId == null || tenantId.isBlank() ? TenantContext.requireTenantId() : tenantId;
    }
}
