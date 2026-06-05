package com.mw.ai.agi.bot.domain;

import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;

public record BotRunResult(AiBot bot, WorkflowExecutionResult execution) {
}
