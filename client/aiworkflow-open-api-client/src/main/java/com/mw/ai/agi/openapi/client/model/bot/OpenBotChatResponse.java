package com.mw.ai.agi.openapi.client.model.bot;

import com.mw.ai.agi.openapi.client.model.workflow.WorkflowExecutionResponse;

import java.util.List;

public record OpenBotChatResponse(
        BotSession session,
        List<BotMessage> messages,
        BotMessage reply,
        WorkflowExecutionResponse execution
) {
}
