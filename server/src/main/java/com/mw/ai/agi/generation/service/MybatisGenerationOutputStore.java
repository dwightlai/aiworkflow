package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationOutput;
import com.mw.ai.agi.generation.persistence.GenerationOutputEntity;
import com.mw.ai.agi.generation.persistence.GenerationOutputMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

public class MybatisGenerationOutputStore implements GenerationOutputStore {
    private final GenerationOutputMapper mapper;

    public MybatisGenerationOutputStore(GenerationOutputMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public GenerationOutput save(GenerationOutput output) {
        GenerationOutputEntity entity = toEntity(output);
        if (mapper.selectById(output.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return output;
    }

    @Override
    public Optional<GenerationOutput> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<GenerationOutput> listByJobId(String jobId) {
        return mapper.selectList(new LambdaQueryWrapper<GenerationOutputEntity>()
                        .eq(GenerationOutputEntity::getJobId, jobId)
                        .orderByAsc(GenerationOutputEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private GenerationOutputEntity toEntity(GenerationOutput output) {
        GenerationOutputEntity entity = new GenerationOutputEntity();
        entity.setId(output.id());
        entity.setTenantId(output.tenantId());
        entity.setJobId(output.jobId());
        entity.setTitle(output.title());
        entity.setOutputType(output.outputType());
        entity.setContentMarkdown(output.contentMarkdown());
        entity.setContentJson(output.contentJson());
        entity.setContentDocxPath(output.contentDocxPath());
        entity.setOutputTemplateId(output.outputTemplateId());
        entity.setCitations(output.citations());
        entity.setSourceSnapshot(output.sourceSnapshot());
        entity.setStatus(output.status());
        entity.setCreatedAt(output.createdAt());
        return entity;
    }

    private GenerationOutput toDomain(GenerationOutputEntity entity) {
        return new GenerationOutput(
                entity.getId(),
                entity.getTenantId(),
                entity.getJobId(),
                entity.getTitle(),
                entity.getOutputType(),
                entity.getContentMarkdown(),
                entity.getContentJson(),
                entity.getContentDocxPath(),
                entity.getOutputTemplateId(),
                entity.getCitations(),
                entity.getSourceSnapshot(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
