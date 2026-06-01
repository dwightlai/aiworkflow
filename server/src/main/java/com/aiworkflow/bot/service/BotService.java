package com.aiworkflow.bot.service;

import com.aiworkflow.bot.domain.AiBot;
import com.aiworkflow.bot.domain.BotChatResult;
import com.aiworkflow.bot.domain.BotMessage;
import com.aiworkflow.bot.domain.BotMessageRole;
import com.aiworkflow.bot.domain.BotRunResult;
import com.aiworkflow.bot.domain.BotSession;
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
        Map<String, Object> executionInput = executionInput(bot, defaultString(message, ""), input, List.of());
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

    public List<BotSession> listSessions(String botId) {
        get(botId);
        return store.listSessions(botId);
    }

    public List<BotMessage> listMessages(String botId, String sessionId) {
        get(botId);
        ensureSession(botId, sessionId);
        return store.listMessages(botId, sessionId);
    }

    public BotChatResult chat(String botId, String sessionId, String message, Map<String, Object> input) {
        AiBot bot = get(botId);
        if (bot.status() != BotStatus.ENABLED) {
            throw new IllegalStateException("Bot is disabled: " + botId);
        }
        Instant now = Instant.now();
        BotSession session = sessionId == null || sessionId.isBlank()
                ? store.saveSession(new BotSession(
                        "session_" + UUID.randomUUID(),
                        bot.id(),
                        title(message),
                        0,
                        now,
                        now
                ))
                : ensureSession(bot.id(), sessionId);

        List<BotMessage> history = store.listMessages(bot.id(), session.id());
        BotMessage userMessage = store.saveMessage(new BotMessage(
                "msg_" + UUID.randomUUID(),
                session.id(),
                bot.id(),
                BotMessageRole.USER,
                defaultString(message, ""),
                Instant.now()
        ));
        Map<String, Object> executionInput = executionInput(bot, userMessage.content(), input, history);
        WorkflowExecutionResult execution = executionService.runWorkflow(new WorkflowExecutionRequest(bot.workflowId(), executionInput));
        BotMessage assistantMessage = store.saveMessage(new BotMessage(
                "msg_" + UUID.randomUUID(),
                session.id(),
                bot.id(),
                BotMessageRole.ASSISTANT,
                replyText(execution.execution().output()),
                Instant.now()
        ));
        List<BotMessage> messages = store.listMessages(bot.id(), session.id());
        BotSession updatedSession = store.saveSession(new BotSession(
                session.id(),
                session.botId(),
                session.title(),
                messages.size(),
                session.createdAt(),
                Instant.now()
        ));
        store.save(new AiBot(
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
        return new BotChatResult(updatedSession, messages, assistantMessage, execution);
    }

    private AiBot get(String id) {
        return store.findById(id).orElseThrow(() -> new BotNotFoundException(id));
    }

    private BotSession ensureSession(String botId, String sessionId) {
        return store.findSessionById(botId, sessionId)
                .orElseThrow(() -> new BotNotFoundException(sessionId));
    }

    private Map<String, Object> executionInput(AiBot bot, String message, Map<String, Object> input, List<BotMessage> history) {
        Map<String, Object> executionInput = new LinkedHashMap<>();
        if (input != null) {
            executionInput.putAll(input);
        }
        executionInput.put("message", message);
        executionInput.put("history", history.stream()
                .map(item -> Map.of("role", item.role().name(), "content", item.content()))
                .toList());
        executionInput.put("botId", bot.id());
        executionInput.put("botName", bot.name());
        executionInput.put("systemPrompt", bot.systemPrompt());
        if (bot.modelProviderId() != null) {
            executionInput.put("modelProviderId", bot.modelProviderId());
        }
        if (bot.knowledgeBaseId() != null) {
            executionInput.put("knowledgeBaseId", bot.knowledgeBaseId());
        }
        return executionInput;
    }

    private String replyText(Map<String, Object> output) {
        Object answer = output.get("answer");
        if (answer != null) {
            return String.valueOf(answer);
        }
        Object message = output.get("message");
        if (message != null) {
            return String.valueOf(message);
        }
        return output.toString();
    }

    private String title(String message) {
        String value = defaultString(message, "新会话").trim();
        return value.length() > 30 ? value.substring(0, 30) : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
