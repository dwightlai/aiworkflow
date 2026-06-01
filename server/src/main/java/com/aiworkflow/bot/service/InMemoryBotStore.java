package com.aiworkflow.bot.service;

import com.aiworkflow.bot.domain.AiBot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBotStore implements BotStore {
    private final Map<String, AiBot> bots = new ConcurrentHashMap<>();

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
    }
}
