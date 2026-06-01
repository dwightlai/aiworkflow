package com.aiworkflow.bot.domain;

import com.aiworkflow.workflow.engine.WorkflowExecutionResult;

import java.util.List;

public record BotChatResult(
        BotSession session,
        List<BotMessage> messages,
        BotMessage reply,
        WorkflowExecutionResult execution
) {
}
