package com.mw.ai.agi.prompt.service;

import com.mw.ai.agi.prompt.domain.PromptTemplate;
import com.mw.ai.agi.prompt.persistence.PromptTemplateEntity;
import com.mw.ai.agi.prompt.persistence.PromptTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

public class MybatisPromptTemplateStore implements PromptTemplateStore {
    private final PromptTemplateMapper mapper;

    public MybatisPromptTemplateStore(PromptTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PromptTemplate save(PromptTemplate template) {
        PromptTemplateEntity entity = toEntity(template);
        if (mapper.selectById(template.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return template;
    }

    @Override
    public Optional<PromptTemplate> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<PromptTemplate> list(String tenantId) {
        LambdaQueryWrapper<PromptTemplateEntity> wrapper = new LambdaQueryWrapper<PromptTemplateEntity>()
                .orderByAsc(PromptTemplateEntity::getCreatedAt);
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(PromptTemplateEntity::getTenantId, tenantId);
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }

    private PromptTemplateEntity toEntity(PromptTemplate template) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setId(template.id());
        entity.setTenantId(template.tenantId());
        entity.setName(template.name());
        entity.setTemplate(template.template());
        entity.setDescription(template.description());
        entity.setCreatedAt(template.createdAt());
        entity.setUpdatedAt(template.updatedAt());
        return entity;
    }

    private PromptTemplate toDomain(PromptTemplateEntity entity) {
        return new PromptTemplate(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getTemplate(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
