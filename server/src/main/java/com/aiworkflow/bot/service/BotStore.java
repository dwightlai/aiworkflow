package com.aiworkflow.bot.service;

import com.aiworkflow.bot.domain.AiBot;
import com.aiworkflow.bot.domain.BotMessage;
import com.aiworkflow.bot.domain.BotSession;

import java.util.List;
import java.util.Optional;

public interface BotStore {
    AiBot save(AiBot bot);

    List<AiBot> list();

    Optional<AiBot> findById(String id);

    void delete(String id);

    BotSession saveSession(BotSession session);

    List<BotSession> listSessions(String botId);

    Optional<BotSession> findSessionById(String botId, String sessionId);

    BotMessage saveMessage(BotMessage message);

    List<BotMessage> listMessages(String botId, String sessionId);
}
