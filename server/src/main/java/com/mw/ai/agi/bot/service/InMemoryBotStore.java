package com.mw.ai.agi.bot.service;

import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBotStore implements BotStore {
    private final Map<String, AiBot> bots = new ConcurrentHashMap<>();
    private final Map<String, BotSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, BotMessage> messages = new ConcurrentHashMap<>();

    @Override
    public AiBot save(AiBot bot) {
        bots.put(bot.id(), bot);
        return bot;
    }

    @Override
    public List<AiBot> list() {
        return new ArrayList<>(bots.values()).stream()
                .sorted(Comparator.comparing(AiBot::createdAt))
                .toList();
    }

    @Override
    public Optional<AiBot> findById(String id) {
        return Optional.ofNullable(bots.get(id));
    }

    @Override
    public void delete(String id) {
        bots.remove(id);
        sessions.values().removeIf(session -> session.botId().equals(id));
        messages.values().removeIf(message -> message.botId().equals(id));
    }

    @Override
    public BotSession saveSession(BotSession session) {
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public List<BotSession> listSessions(String botId) {
        return sessions.values().stream()
                .filter(session -> session.botId().equals(botId))
                .sorted(Comparator.comparing(BotSession::updatedAt).reversed())
                .toList();
    }

    @Override
    public Optional<BotSession> findSessionById(String botId, String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId))
                .filter(session -> session.botId().equals(botId));
    }

    @Override
    public BotMessage saveMessage(BotMessage message) {
        messages.put(message.id(), message);
        return message;
    }

    @Override
    public List<BotMessage> listMessages(String botId, String sessionId) {
        return messages.values().stream()
                .filter(message -> message.botId().equals(botId) && message.sessionId().equals(sessionId))
                .sorted(Comparator.comparing(BotMessage::createdAt))
                .toList();
    }
}
