package com.mw.ai.agi.openapi.client.model.bot;

import com.mw.ai.agi.openapi.client.model.workflow.WorkflowExecutionResponse;

public record OpenBotRunResponse(OpenBotView bot, WorkflowExecutionResponse execution) {
}
