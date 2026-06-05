package com.mw.ai.agi.bot.domain;

import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;

import java.util.List;

public record BotChatResult(
        BotSession session,
        List<BotMessage> messages,
        BotMessage reply,
        WorkflowExecutionResult execution
) {
}
