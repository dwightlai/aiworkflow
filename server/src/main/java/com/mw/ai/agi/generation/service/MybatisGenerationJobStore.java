package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationJob;
import com.mw.ai.agi.generation.persistence.GenerationJobEntity;
import com.mw.ai.agi.generation.persistence.GenerationJobMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

public class MybatisGenerationJobStore implements GenerationJobStore {
    private final GenerationJobMapper mapper;

    public MybatisGenerationJobStore(GenerationJobMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public GenerationJob save(GenerationJob job) {
        GenerationJobEntity entity = toEntity(job);
        if (mapper.selectById(job.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return job;
    }

    @Override
    public Optional<GenerationJob> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<GenerationJob> list(String tenantId) {
        LambdaQueryWrapper<GenerationJobEntity> wrapper = new LambdaQueryWrapper<GenerationJobEntity>()
                .orderByDesc(GenerationJobEntity::getCreatedAt);
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(GenerationJobEntity::getTenantId, tenantId);
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private GenerationJobEntity toEntity(GenerationJob job) {
        GenerationJobEntity entity = new GenerationJobEntity();
        entity.setId(job.id());
        entity.setTenantId(job.tenantId());
        entity.setBotId(job.botId());
        entity.setTemplateId(job.templateId());
        entity.setWorkflowId(job.workflowId());
        entity.setUnitId(job.unitId());
        entity.setUserId(job.userId());
        entity.setKnowledgeBaseIds(job.knowledgeBaseIds());
        entity.setExternalCorpusRef(job.externalCorpusRef());
        entity.setVariables(job.variables());
        entity.setStatus(job.status());
        entity.setOutlineJson(job.outlineJson());
        entity.setSectionOutputsJson(job.sectionOutputsJson());
        entity.setWorkflowRunSnapshot(job.workflowRunSnapshot());
        entity.setErrorMessage(job.errorMessage());
        entity.setStartedAt(job.startedAt());
        entity.setCompletedAt(job.completedAt());
        entity.setCreatedAt(job.createdAt());
        return entity;
    }

    private GenerationJob toDomain(GenerationJobEntity entity) {
        return new GenerationJob(
                entity.getId(),
                entity.getTenantId(),
                entity.getBotId(),
                entity.getTemplateId(),
                entity.getWorkflowId(),
                entity.getUnitId(),
                entity.getUserId(),
                entity.getKnowledgeBaseIds(),
                entity.getExternalCorpusRef(),
                entity.getVariables(),
                entity.getStatus(),
                entity.getOutlineJson(),
                entity.getSectionOutputsJson(),
                entity.getWorkflowRunSnapshot(),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getCreatedAt()
        );
    }
}
