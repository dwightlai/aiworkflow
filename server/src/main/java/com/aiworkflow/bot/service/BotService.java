package com.aiworkflow.bot.service;

import com.aiworkflow.bot.domain.AiBot;
import com.aiworkflow.bot.domain.BotRunResult;
import com.aiworkflow.bot.domain.BotStatus;
import com.aiworkflow.workflow.engine.WorkflowExecutionRequest;
import com.aiworkflow.workflow.engine.WorkflowExecutionResult;
import com.aiworkflow.workflow.engine.WorkflowExecutionService;
import com.aiworkflow.workflow.service.WorkflowApplicationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BotService {
    private final BotStore store;
    private final WorkflowApplicationService workflowService;
    private final WorkflowExecutionService executionService;

    public BotService(
            BotStore store,
            WorkflowApplicationService workflowService,
            WorkflowExecutionService executionService
    ) {
        this.store = store;
        this.workflowService = workflowService;
        this.executionService = executionService;
    }

    public List<AiBot> list() {
        return store.list();
    }

    public AiBot create(
            String name,
            String description,
            String avatar,
            String workflowId,
            String modelProviderId,
            String knowledgeBaseId,
            String systemPrompt,
            String openingMessage,
            BotStatus status
    ) {
        workflowService.getWorkflow(workflowId);
        Instant now = Instant.now();
        BotStatus effectiveStatus = status == null ? BotStatus.ENABLED : status;
        return store.save(new AiBot(
                "bot_" + UUID.randomUUID(),
                name,
                description,
                defaultString(avatar, "robot"),
                workflowId,
                blankToNull(modelProviderId),
                blankToNull(knowledgeBaseId),
                defaultString(systemPrompt, ""),
                defaultString(openingMessage, ""),
                effectiveStatus,
                0,
                effectiveStatus == BotStatus.ENABLED ? now : null,
                now,
                now
        ));
    }

    public AiBot update(
            String id,
            String name,
            String description,
            String avatar,
            String workflowId,
            String modelProviderId,
            String knowledgeBaseId,
            String systemPrompt,
            String openingMessage,
            BotStatus status
    ) {
        AiBot current = get(id);
        workflowService.getWorkflow(workflowId);
        BotStatus effectiveStatus = status == null ? current.status() : status;
        Instant now = Instant.now();
        return store.save(new AiBot(
                current.id(),
                defaultString(name, current.name()),
                description,
                defaultString(avatar, current.avatar()),
                workflowId,
                blankToNull(modelProviderId),
                blankToNull(knowledgeBaseId),
                defaultString(systemPrompt, ""),
                defaultString(openingMessage, ""),
                effectiveStatus,
                current.conversationCount(),
                effectiveStatus == BotStatus.ENABLED && current.publishedAt() == null ? now : current.publishedAt(),
                current.createdAt(),
                now
        ));
    }

    public void delete(String id) {
        get(id);
        store.delete(id);
    }

    public BotRunResult run(String id, String message, Map<String, Object> input) {
        AiBot bot = get(id);
        if (bot.status() != BotStatus.ENABLED) {
            throw new IllegalStateException("Bot is disabled: " + id);
        }
        Map<String, Object> executionInput = new LinkedHashMap<>();
        if (input != null) {
            executionInput.putAll(input);
        }
        executionInput.put("message", defaultString(message, ""));
        executionInput.put("botId", bot.id());
        executionInput.put("botName", bot.name());
        executionInput.put("systemPrompt", bot.systemPrompt());
        if (bot.modelProviderId() != null) {
            executionInput.put("modelProviderId", bot.modelProviderId());
        }
        if (bot.knowledgeBaseId() != null) {
            executionInput.put("knowledgeBaseId", bot.knowledgeBaseId());
        }

        WorkflowExecutionResult execution = executionService.runWorkflow(new WorkflowExecutionRequest(bot.workflowId(), executionInput));
        AiBot updated = store.save(new AiBot(
                bot.id(),
                bot.name(),
                bot.description(),
                bot.avatar(),
                bot.workflowId(),
                bot.modelProviderId(),
                bot.knowledgeBaseId(),
                bot.systemPrompt(),
                bot.openingMessage(),
                bot.status(),
                bot.conversationCount() + 1,
                bot.publishedAt(),
                bot.createdAt(),
                Instant.now()
        ));
        return new BotRunResult(updated, execution);
    }

    private AiBot get(String id) {
        return store.findById(id).orElseThrow(() -> new BotNotFoundException(id));
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
