package com.mw.ai.agi.model.service;

import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.persistence.ModelProviderEntity;
import com.mw.ai.agi.model.persistence.ModelProviderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

import com.mw.ai.agi.auth.service.TenantContext;

public class MybatisModelProviderStore implements ModelProviderStore {
    private final ModelProviderMapper mapper;

    public MybatisModelProviderStore(ModelProviderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ModelProvider save(ModelProvider provider) {
        ModelProviderEntity entity = toEntity(provider);
        if (mapper.selectById(provider.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return provider;
    }

    @Override
    public Optional<ModelProvider> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ModelProvider> list(String tenantId) {
        LambdaQueryWrapper<ModelProviderEntity> wrapper = new LambdaQueryWrapper<ModelProviderEntity>()
                .orderByAsc(ModelProviderEntity::getCreatedAt);
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(ModelProviderEntity::getTenantId, tenantId);
        }
        return mapper.selectList(wrapper)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }

    private ModelProviderEntity toEntity(ModelProvider provider) {
        ModelProviderEntity entity = new ModelProviderEntity();
        entity.setId(provider.id());
        entity.setTenantId(provider.tenantId());
        entity.setName(provider.name());
        entity.setModelType(provider.modelType());
        entity.setModelUsage(provider.modelUsage());
        entity.setDescription(provider.description());
        entity.setVisionSupport(provider.visionSupport());
        entity.setPricePerMillionTokens(provider.pricePerMillionTokens());
        entity.setBaseUrl(provider.baseUrl());
        entity.setModel(provider.model());
        entity.setApiKeyRef(provider.apiKeyRef());
        entity.setEnabled(provider.enabled());
        entity.setCreatedAt(provider.createdAt());
        entity.setUpdatedAt(provider.updatedAt());
        return entity;
    }

    private ModelProvider toDomain(ModelProviderEntity entity) {
        return new ModelProvider(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getModelType(),
                entity.getModelUsage(),
                entity.getDescription(),
                Boolean.TRUE.equals(entity.getVisionSupport()),
                entity.getPricePerMillionTokens(),
                entity.getBaseUrl(),
                entity.getModel(),
                entity.getApiKeyRef(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
