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

import java.util.List;
import java.util.Optional;

public class MybatisBotStore implements BotStore {
    private final AiBotMapper botMapper;
    private final BotSessionMapper sessionMapper;
    private final BotMessageMapper messageMapper;

    public MybatisBotStore(AiBotMapper botMapper, BotSessionMapper sessionMapper, BotMessageMapper messageMapper) {
        this.botMapper = botMapper;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
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
    public List<AiBot> list() {
        return botMapper.selectList(new LambdaQueryWrapper<AiBotEntity>()
                        .orderByAsc(AiBotEntity::getCreatedAt))
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
        entity.setName(bot.name());
        entity.setDescription(bot.description());
        entity.setAvatar(bot.avatar());
        entity.setWorkflowId(bot.workflowId());
        entity.setModelProviderId(bot.modelProviderId());
        entity.setKnowledgeBaseId(bot.knowledgeBaseId());
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
                entity.getName(),
                entity.getDescription(),
                entity.getAvatar(),
                entity.getWorkflowId(),
                entity.getModelProviderId(),
                entity.getKnowledgeBaseId(),
                entity.getSystemPrompt(),
                entity.getOpeningMessage(),
                BotStatus.valueOf(entity.getStatus()),
                entity.getConversationCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
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
