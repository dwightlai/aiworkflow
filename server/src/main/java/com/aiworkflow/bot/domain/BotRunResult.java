package com.aiworkflow.bot.domain;

import com.aiworkflow.workflow.engine.WorkflowExecutionResult;

public record BotRunResult(AiBot bot, WorkflowExecutionResult execution) {
}
