package com.mw.ai.agi.chat.domain;

import java.time.Instant;

public record AgentJob(
        String id,
        String botId,
        String conversationId,
        String sourceJobId,
        String jobType,
        String status,
        Integer progress,
        String currentStep,
        String result,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
