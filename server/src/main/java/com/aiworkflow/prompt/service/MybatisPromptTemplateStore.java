package com.aiworkflow.prompt.service;

import com.aiworkflow.prompt.domain.PromptTemplate;
import com.aiworkflow.prompt.persistence.PromptTemplateEntity;
import com.aiworkflow.prompt.persistence.PromptTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

public class MybatisPromptTemplateStore implements PromptTemplateStore {
    private static final String DEFAULT_TENANT_ID = "default";

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
    public List<PromptTemplate> list() {
        return mapper.selectList(new LambdaQueryWrapper<PromptTemplateEntity>()
                        .orderByAsc(PromptTemplateEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }

    private PromptTemplateEntity toEntity(PromptTemplate template) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setId(template.id());
        entity.setTenantId(DEFAULT_TENANT_ID);
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
                entity.getName(),
                entity.getTemplate(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
