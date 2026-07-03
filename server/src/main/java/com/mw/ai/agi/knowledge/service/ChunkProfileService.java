package com.mw.ai.agi.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.knowledge.chunking.ChunkingQualityReport;
import com.mw.ai.agi.knowledge.domain.ChunkProfileVersion;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import com.mw.ai.agi.knowledge.domain.KnowledgeDocument;
import com.mw.ai.agi.knowledge.persistence.ChunkProfileVersionEntity;
import com.mw.ai.agi.knowledge.persistence.ChunkProfileVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkProfileService {
    private final ChunkProfileVersionMapper mapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentSplitter splitter;

    public ChunkProfileService(
            ChunkProfileVersionMapper mapper,
            KnowledgeBaseService knowledgeBaseService,
            KnowledgeDocumentSplitter splitter
    ) {
        this.mapper = mapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.splitter = splitter;
    }

    public List<ChunkProfileVersion> list(String knowledgeBaseId) {
        knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        return mapper.selectList(new LambdaQueryWrapper<ChunkProfileVersionEntity>()
                        .eq(ChunkProfileVersionEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(ChunkProfileVersionEntity::getVersion))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    public ChunkProfileVersion create(
            String knowledgeBaseId,
            String name,
            String strategy,
            int chunkSize,
            int chunkOverlap,
            String configJson
    ) {
        KnowledgeBase base = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        int nextVersion = list(knowledgeBaseId).stream()
                .map(ChunkProfileVersion::version)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
        ChunkProfileVersionEntity entity = new ChunkProfileVersionEntity();
        entity.setId("profile_" + UUID.randomUUID());
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setVersion(nextVersion);
        entity.setName(name == null || name.isBlank() ? "Profile V" + nextVersion : name.trim());
        entity.setStrategy(strategy == null || strategy.isBlank() ? base.splitterType() : strategy.trim().toUpperCase());
        entity.setChunkSize(chunkSize <= 0 ? base.chunkSize() : chunkSize);
        entity.setChunkOverlap(Math.max(0, chunkOverlap));
        entity.setConfigJson(configJson == null || configJson.isBlank() ? "{}" : configJson);
        entity.setStatus("DRAFT");
        entity.setCreatedAt(Instant.now());
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Transactional
    public ChunkProfileVersion activate(String knowledgeBaseId, String profileId) {
        KnowledgeBase base = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        ChunkProfileVersionEntity selected = requireProfile(knowledgeBaseId, profileId);
        for (ChunkProfileVersionEntity entity : mapper.selectList(
                new LambdaQueryWrapper<ChunkProfileVersionEntity>()
                        .eq(ChunkProfileVersionEntity::getKnowledgeBaseId, knowledgeBaseId))) {
            entity.setStatus(entity.getId().equals(profileId) ? "ACTIVE" : "RETIRED");
            mapper.updateById(entity);
        }
        knowledgeBaseService.update(
                base.id(),
                base.name(),
                base.description(),
                base.ownerUnitId(),
                base.embeddingModelId(),
                base.vectorStoreConfigId(),
                base.vectorDimension(),
                selected.getStrategy(),
                selected.getChunkSize(),
                selected.getChunkOverlap(),
                base.retrievalMode(),
                base.topK(),
                semanticSimilarityThreshold(
                        selected.getConfigJson(),
                        base.semanticSimilarityThreshold()
                )
        );
        return toDomain(selectedWithStatus(selected, "ACTIVE"));
    }

    public ProfileComparison compare(
            String knowledgeBaseId,
            String documentId,
            String leftProfileId,
            String rightProfileId
    ) {
        KnowledgeBase base = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        KnowledgeDocument document = knowledgeBaseService.listDocuments(knowledgeBaseId).stream()
                .filter(item -> item.id().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
        ChunkProfileVersion left = toDomain(requireProfile(knowledgeBaseId, leftProfileId));
        ChunkProfileVersion right = toDomain(requireProfile(knowledgeBaseId, rightProfileId));
        return new ProfileComparison(
                preview(document, left, base.embeddingModelId()),
                preview(document, right, base.embeddingModelId())
        );
    }

    private ProfilePreview preview(KnowledgeDocument document, ChunkProfileVersion profile, String embeddingModelId) {
        List<KnowledgeChunkPreview> chunks = splitter.splitDocument(
                document.name(),
                document.rawContent(),
                new KnowledgeSplitRequest(
                        profile.strategy(),
                        profile.chunkSize(),
                        profile.chunkOverlap(),
                        null,
                        embeddingModelId,
                        semanticSimilarityThreshold(profile.configJson(), 0.78)
                )
        );
        return new ProfilePreview(profile, ChunkingQualityReport.from(chunks), chunks);
    }

    private ChunkProfileVersionEntity requireProfile(String knowledgeBaseId, String profileId) {
        ChunkProfileVersionEntity entity = mapper.selectById(profileId);
        if (entity == null || !knowledgeBaseId.equals(entity.getKnowledgeBaseId())) {
            throw new IllegalArgumentException("Chunk profile not found: " + profileId);
        }
        return entity;
    }

    private ChunkProfileVersionEntity selectedWithStatus(ChunkProfileVersionEntity entity, String status) {
        entity.setStatus(status);
        return entity;
    }

    private ChunkProfileVersion toDomain(ChunkProfileVersionEntity entity) {
        return new ChunkProfileVersion(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getVersion(),
                entity.getName(),
                entity.getStrategy(),
                entity.getChunkSize(),
                entity.getChunkOverlap(),
                entity.getConfigJson(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    static double semanticSimilarityThreshold(String configJson, double fallback) {
        if (configJson == null || configJson.isBlank()) {
            return fallback;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode value =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(configJson)
                            .get("semanticSimilarityThreshold");
            if (value == null || !value.isNumber()) {
                return fallback;
            }
            return Math.max(0, Math.min(1, value.asDouble()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return fallback;
        }
    }

    public record ProfilePreview(
            ChunkProfileVersion profile,
            ChunkingQualityReport quality,
            List<KnowledgeChunkPreview> chunks
    ) {
    }

    public record ProfileComparison(ProfilePreview left, ProfilePreview right) {
    }
}
