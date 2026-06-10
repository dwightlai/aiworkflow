package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationTemplate;
import com.mw.ai.agi.generation.persistence.GenerationTemplateEntity;
import com.mw.ai.agi.generation.persistence.GenerationTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

public class MybatisGenerationTemplateStore implements GenerationTemplateStore {
    private final GenerationTemplateMapper mapper;

    public MybatisGenerationTemplateStore(GenerationTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public GenerationTemplate save(GenerationTemplate template) {
        GenerationTemplateEntity entity = toEntity(template);
        if (mapper.selectById(template.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return template;
    }

    @Override
    public Optional<GenerationTemplate> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<GenerationTemplate> list(String tenantId) {
        LambdaQueryWrapper<GenerationTemplateEntity> wrapper = new LambdaQueryWrapper<GenerationTemplateEntity>()
                .orderByAsc(GenerationTemplateEntity::getCreatedAt);
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(GenerationTemplateEntity::getTenantId, tenantId);
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }

    private GenerationTemplateEntity toEntity(GenerationTemplate template) {
        GenerationTemplateEntity entity = new GenerationTemplateEntity();
        entity.setId(template.id());
        entity.setTenantId(template.tenantId());
        entity.setName(template.name());
        entity.setCode(template.code());
        entity.setDescription(template.description());
        entity.setCategory(template.category());
        entity.setOwnerUnitId(template.ownerUnitId());
        entity.setOutputType(template.outputType());
        entity.setTemplateSchema(template.templateSchema());
        entity.setWorkflowId(template.workflowId());
        entity.setWorkflowSnapshot(template.workflowSnapshot());
        entity.setStatus(template.status());
        entity.setVersion(template.version());
        entity.setCreatedBy(template.createdBy());
        entity.setCreatedAt(template.createdAt());
        entity.setUpdatedAt(template.updatedAt());
        return entity;
    }

    private GenerationTemplate toDomain(GenerationTemplateEntity entity) {
        return new GenerationTemplate(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getOwnerUnitId(),
                entity.getOutputType(),
                entity.getTemplateSchema(),
                entity.getWorkflowId(),
                entity.getWorkflowSnapshot(),
                entity.getStatus(),
                entity.getVersion() == null ? 1 : entity.getVersion(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
