package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.mw.ai.agi.knowledge.persistence.VectorStoreConfigEntity;
import com.mw.ai.agi.knowledge.persistence.VectorStoreConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

public class MybatisVectorStoreConfigStore implements VectorStoreConfigStore {
    private final VectorStoreConfigMapper mapper;

    public MybatisVectorStoreConfigStore(VectorStoreConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public VectorStoreConfig save(VectorStoreConfig config) {
        VectorStoreConfigEntity entity = toEntity(config);
        if (mapper.selectById(config.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return config;
    }

    @Override
    public Optional<VectorStoreConfig> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<VectorStoreConfig> list() {
        return mapper.selectList(new LambdaQueryWrapper<VectorStoreConfigEntity>()
                        .orderByAsc(VectorStoreConfigEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }

    private VectorStoreConfigEntity toEntity(VectorStoreConfig config) {
        VectorStoreConfigEntity entity = new VectorStoreConfigEntity();
        entity.setId(config.id());
        entity.setName(config.name());
        entity.setStoreType(config.storeType());
        entity.setEndpoint(config.endpoint());
        entity.setIndexName(config.indexName());
        entity.setUsername(config.username());
        entity.setPassword(config.password());
        entity.setApiKey(config.apiKey());
        entity.setConnectTimeoutMs(config.connectTimeoutMs());
        entity.setReadTimeoutMs(config.readTimeoutMs());
        entity.setEnabled(config.enabled());
        entity.setCreatedAt(config.createdAt());
        entity.setUpdatedAt(config.updatedAt());
        return entity;
    }

    private VectorStoreConfig toDomain(VectorStoreConfigEntity entity) {
        return new VectorStoreConfig(
                entity.getId(),
                entity.getName(),
                entity.getStoreType(),
                entity.getEndpoint(),
                entity.getIndexName(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getApiKey(),
                entity.getConnectTimeoutMs(),
                entity.getReadTimeoutMs(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
