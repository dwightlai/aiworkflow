package com.mw.ai.agi.bot.service;

import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotMessageRole;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.bot.persistence.AiBotEntity;
import com.mw.ai.agi.bot.persistence.AiBotMapper;
import com.mw.ai.agi.bot.persistence.BotMessageEntity;
import com.mw.ai.agi.bot.persistence.BotMessageMapper;
import com.mw.ai.agi.bot.persistence.BotSessionEntity;
import com.mw.ai.agi.bot.persistence.BotSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.common.asset.AssetReferenceSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mw.ai.agi.persistence.JsonSupport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MybatisBotStore implements BotStore {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final AiBotMapper botMapper;
    private final BotSessionMapper sessionMapper;
    private final BotMessageMapper messageMapper;
    private final JsonSupport jsonSupport;

    public MybatisBotStore(
            AiBotMapper botMapper,
            BotSessionMapper sessionMapper,
            BotMessageMapper messageMapper,
            JsonSupport jsonSupport
    ) {
        this.botMapper = botMapper;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public AiBot save(AiBot bot) {
        AiBotEntity entity = toEntity(bot);
        if (botMapper.selectById(bot.id()) == null) {
            botMapper.insert(entity);
        } else {
            botMapper.updateById(entity);
        }
        return bot;
    }

    @Override
    public List<AiBot> list(String tenantId) {
        LambdaQueryWrapper<AiBotEntity> wrapper = new LambdaQueryWrapper<AiBotEntity>()
                .orderByAsc(AiBotEntity::getCreatedAt);
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(AiBotEntity::getTenantId, tenantId);
        }
        return botMapper.selectList(wrapper)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<AiBot> findById(String id) {
        return Optional.ofNullable(botMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public void delete(String id) {
        botMapper.deleteById(id);
    }

    @Override
    public BotSession saveSession(BotSession session) {
        BotSessionEntity entity = toEntity(session);
        if (sessionMapper.selectById(session.id()) == null) {
            sessionMapper.insert(entity);
        } else {
            sessionMapper.updateById(entity);
        }
        return session;
    }

    @Override
    public List<BotSession> listSessions(String botId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<BotSessionEntity>()
                        .eq(BotSessionEntity::getBotId, botId)
                        .orderByDesc(BotSessionEntity::getUpdatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<BotSession> findSessionById(String botId, String sessionId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<BotSessionEntity>()
                        .eq(BotSessionEntity::getBotId, botId)
                        .eq(BotSessionEntity::getId, sessionId))
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public BotMessage saveMessage(BotMessage message) {
        messageMapper.insert(toEntity(message));
        return message;
    }

    @Override
    public List<BotMessage> listMessages(String botId, String sessionId) {
        return messageMapper.selectList(new LambdaQueryWrapper<BotMessageEntity>()
                        .eq(BotMessageEntity::getBotId, botId)
                        .eq(BotMessageEntity::getSessionId, sessionId)
                        .orderByAsc(BotMessageEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AiBotEntity toEntity(AiBot bot) {
        AiBotEntity entity = new AiBotEntity();
        entity.setId(bot.id());
        entity.setTenantId(bot.tenantId());
        entity.setName(bot.name());
        entity.setDescription(bot.description());
        entity.setOwnerUnitId(bot.ownerUnitId());
        entity.setAvatar(bot.avatar());
        entity.setWorkflowId(bot.workflowId());
        entity.setModelProviderId(bot.modelProviderId());
        entity.setKnowledgeBaseIds(writeKnowledgeBaseIds(bot.knowledgeBaseIds()));
        entity.setSystemPrompt(bot.systemPrompt());
        entity.setOpeningMessage(bot.openingMessage());
        entity.setStatus(bot.status().name());
        entity.setConversationCount(bot.conversationCount());
        entity.setPublishedAt(bot.publishedAt());
        entity.setCreatedAt(bot.createdAt());
        entity.setUpdatedAt(bot.updatedAt());
        return entity;
    }

    private AiBot toDomain(AiBotEntity entity) {
        return new AiBot(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwnerUnitId(),
                entity.getAvatar(),
                entity.getWorkflowId(),
                entity.getModelProviderId(),
                readKnowledgeBaseIds(entity.getKnowledgeBaseIds()),
                entity.getSystemPrompt(),
                entity.getOpeningMessage(),
                BotStatus.valueOf(entity.getStatus()),
                entity.getConversationCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String writeKnowledgeBaseIds(List<String> knowledgeBaseIds) {
        List<String> normalized = normalizeKnowledgeBaseIds(knowledgeBaseIds);
        return normalized.isEmpty() ? null : jsonSupport.write(normalized);
    }

    private List<String> readKnowledgeBaseIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return normalizeKnowledgeBaseIds(jsonSupport.read(value, STRING_LIST_TYPE));
    }

    private List<String> normalizeKnowledgeBaseIds(List<String> knowledgeBaseIds) {
        return AssetReferenceSupport.requireAssetIds(knowledgeBaseIds);
    }

    private BotSessionEntity toEntity(BotSession session) {
        BotSessionEntity entity = new BotSessionEntity();
        entity.setId(session.id());
        entity.setBotId(session.botId());
        entity.setTitle(session.title());
        entity.setMessageCount(session.messageCount());
        entity.setCreatedAt(session.createdAt());
        entity.setUpdatedAt(session.updatedAt());
        return entity;
    }

    private BotSession toDomain(BotSessionEntity entity) {
        return new BotSession(
                entity.getId(),
                entity.getBotId(),
                entity.getTitle(),
                entity.getMessageCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BotMessageEntity toEntity(BotMessage message) {
        BotMessageEntity entity = new BotMessageEntity();
        entity.setId(message.id());
        entity.setSessionId(message.sessionId());
        entity.setBotId(message.botId());
        entity.setRole(message.role().name());
        entity.setContent(message.content());
        entity.setCreatedAt(message.createdAt());
        return entity;
    }

    private BotMessage toDomain(BotMessageEntity entity) {
        return new BotMessage(
                entity.getId(),
                entity.getSessionId(),
                entity.getBotId(),
                BotMessageRole.valueOf(entity.getRole()),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
