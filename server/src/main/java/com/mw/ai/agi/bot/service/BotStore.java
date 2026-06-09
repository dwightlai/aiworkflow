package com.mw.ai.agi.bot.service;

import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotSession;

import java.util.List;
import java.util.Optional;

public interface BotStore {
    AiBot save(AiBot bot);

    List<AiBot> list(String tenantId);

    Optional<AiBot> findById(String id);

    void delete(String id);

    BotSession saveSession(BotSession session);

    List<BotSession> listSessions(String botId);

    Optional<BotSession> findSessionById(String botId, String sessionId);

    BotMessage saveMessage(BotMessage message);

    List<BotMessage> listMessages(String botId, String sessionId);
}
