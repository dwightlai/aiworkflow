package com.aiworkflow.bot.service;

public class BotNotFoundException extends RuntimeException {
    public BotNotFoundException(String id) {
        super("Bot not found: " + id);
    }
}
