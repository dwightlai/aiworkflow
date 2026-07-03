package com.mw.ai.agi.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.knowledge.persistence.KnowledgeFailureSampleEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeFailureSampleMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeFailureSampleService {
    private final KnowledgeFailureSampleMapper mapper;
    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeFailureSampleService(
            KnowledgeFailureSampleMapper mapper,
            KnowledgeBaseService knowledgeBaseService
    ) {
        this.mapper = mapper;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public List<FailureSample> list(String knowledgeBaseId, String status) {
        knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        LambdaQueryWrapper<KnowledgeFailureSampleEntity> query =
                new LambdaQueryWrapper<KnowledgeFailureSampleEntity>()
                        .eq(KnowledgeFailureSampleEntity::getTenantId, TenantContext.requireTenantId())
                        .eq(KnowledgeFailureSampleEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(KnowledgeFailureSampleEntity::getCreatedAt);
        if (status != null && !status.isBlank()) {
            query.eq(KnowledgeFailureSampleEntity::getStatus, status.trim().toUpperCase());
        }
        return mapper.selectList(query).stream().map(this::toDomain).toList();
    }

    public FailureSample archive(String knowledgeBaseId, ArchiveRequest request) {
        knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        Instant now = Instant.now();
        KnowledgeFailureSampleEntity entity = new KnowledgeFailureSampleEntity();
        entity.setId("failure_" + UUID.randomUUID());
        entity.setTenantId(TenantContext.requireTenantId());
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setQuestion(requireText(request.question(), "question"));
        entity.setRoleSnapshot(request.roleSnapshot());
        entity.setSeedChunkIds(request.seedChunkIds());
        entity.setEvidenceGroupJson(request.evidenceGroupJson());
        entity.setExpectedEvidence(request.expectedEvidence());
        entity.setExpectedAnswerPoints(request.expectedAnswerPoints());
        entity.setFailureFlagsJson(request.failureFlagsJson());
        entity.setParserVersion(request.parserVersion());
        entity.setProfileVersion(request.profileVersion());
        entity.setStatus("OPEN");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        return toDomain(entity);
    }

    public FailureSample updateStatus(String knowledgeBaseId, String sampleId, String status) {
        KnowledgeFailureSampleEntity entity = mapper.selectById(sampleId);
        if (entity == null || !knowledgeBaseId.equals(entity.getKnowledgeBaseId())
                || !TenantContext.requireTenantId().equals(entity.getTenantId())) {
            throw new IllegalArgumentException("Failure sample not found: " + sampleId);
        }
        entity.setStatus(requireText(status, "status").trim().toUpperCase());
        entity.setUpdatedAt(Instant.now());
        mapper.updateById(entity);
        return toDomain(entity);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private FailureSample toDomain(KnowledgeFailureSampleEntity entity) {
        return new FailureSample(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getQuestion(),
                entity.getRoleSnapshot(),
                entity.getSeedChunkIds(),
                entity.getEvidenceGroupJson(),
                entity.getExpectedEvidence(),
                entity.getExpectedAnswerPoints(),
                entity.getFailureFlagsJson(),
                entity.getParserVersion(),
                entity.getProfileVersion(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public record ArchiveRequest(
            String question,
            String roleSnapshot,
            String seedChunkIds,
            String evidenceGroupJson,
            String expectedEvidence,
            String expectedAnswerPoints,
            String failureFlagsJson,
            String parserVersion,
            String profileVersion
    ) {
    }

    public record FailureSample(
            String id,
            String knowledgeBaseId,
            String question,
            String roleSnapshot,
            String seedChunkIds,
            String evidenceGroupJson,
            String expectedEvidence,
            String expectedAnswerPoints,
            String failureFlagsJson,
            String parserVersion,
            String profileVersion,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
