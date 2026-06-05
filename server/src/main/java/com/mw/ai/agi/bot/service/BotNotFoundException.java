package com.mw.ai.agi.bot.service;

public class BotNotFoundException extends RuntimeException {
    public BotNotFoundException(String id) {
        super("Bot not found: " + id);
    }
}
