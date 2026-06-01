package com.aiworkflow.bot.service;

import com.aiworkflow.bot.domain.AiBot;

import java.util.List;
import java.util.Optional;

public interface BotStore {
    AiBot save(AiBot bot);

    List<AiBot> list();

    Optional<AiBot> findById(String id);

    void delete(String id);
}
