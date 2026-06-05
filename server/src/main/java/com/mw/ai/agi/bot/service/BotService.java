package com.mw.ai.agi.bot.service;

import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotChatResult;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotMessageRole;
import com.mw.ai.agi.bot.domain.BotRunResult;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import com.mw.ai.agi.knowledge.service.KnowledgeBaseService;
import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.service.ChatModelClient;
import com.mw.ai.agi.model.service.ModelProviderService;
import com.mw.ai.agi.workflow.engine.WorkflowExecution;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionRequest;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionService;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionStatus;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BotService {
    private final BotStore store;
    private final WorkflowApplicationService workflowService;
    private final WorkflowExecutionService executionService;
    private final ModelProviderService modelProviderService;
    private final ChatModelClient chatModelClient;
    private final KnowledgeBaseService knowledgeBaseService;

    public BotService(
            BotStore store,
            WorkflowApplicationService workflowService,
            WorkflowExecutionService executionService,
            ModelProviderService modelProviderService,
            ChatModelClient chatModelClient,
            KnowledgeBaseService knowledgeBaseService
    ) {
        this.store = store;
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.modelProviderService = modelProviderService;
        this.chatModelClient = chatModelClient;
        this.knowledgeBaseService = knowledgeBaseService;
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
        ensureRunnable(workflowId, modelProviderId, knowledgeBaseId);
        Instant now = Instant.now();
        BotStatus effectiveStatus = status == null ? BotStatus.ENABLED : status;
        return store.save(new AiBot(
                "bot_" + UUID.randomUUID(),
                name,
                description,
                defaultString(avatar, "robot"),
                blankToNull(workflowId),
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
        ensureRunnable(workflowId, modelProviderId, knowledgeBaseId);
        BotStatus effectiveStatus = status == null ? current.status() : status;
        Instant now = Instant.now();
        return store.save(new AiBot(
                current.id(),
                defaultString(name, current.name()),
                description,
                defaultString(avatar, current.avatar()),
                blankToNull(workflowId),
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
        WorkflowExecutionResult execution = runBotLogic(bot, executionInput, defaultString(message, ""), List.of());
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
        Instant userMessageCreatedAt = Instant.now();
        BotMessage userMessage = store.saveMessage(new BotMessage(
                "msg_" + UUID.randomUUID(),
                session.id(),
                bot.id(),
                BotMessageRole.USER,
                defaultString(message, ""),
                userMessageCreatedAt
        ));
        Map<String, Object> executionInput = executionInput(bot, userMessage.content(), input, history);
        WorkflowExecutionResult execution = runBotLogic(bot, executionInput, userMessage.content(), history);
        BotMessage assistantMessage = store.saveMessage(new BotMessage(
                "msg_" + UUID.randomUUID(),
                session.id(),
                bot.id(),
                BotMessageRole.ASSISTANT,
                replyText(execution.execution().output()),
                userMessageCreatedAt.plusNanos(1)
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

    private void ensureRunnable(String workflowId, String modelProviderId, String knowledgeBaseId) {
        if (!isBlank(workflowId)) {
            workflowService.getWorkflow(workflowId);
            return;
        }
        if (isBlank(modelProviderId) && isBlank(knowledgeBaseId)) {
            throw new IllegalArgumentException("Bot requires a workflow, model provider, or knowledge base");
        }
        if (!isBlank(modelProviderId)) {
            ModelProvider provider = modelProviderService.get(modelProviderId);
            if (!provider.enabled()) {
                throw new IllegalArgumentException("Model provider is disabled: " + modelProviderId);
            }
        }
        if (!isBlank(knowledgeBaseId)) {
            knowledgeBaseService.search(knowledgeBaseId, "__health_check__", 1);
        }
    }

    private WorkflowExecutionResult runBotLogic(
            AiBot bot,
            Map<String, Object> executionInput,
            String message,
            List<BotMessage> history
    ) {
        if (!isBlank(bot.workflowId())) {
            return executionService.runWorkflow(new WorkflowExecutionRequest(bot.workflowId(), executionInput));
        }
        return runDirectBot(bot, executionInput, message, history);
    }

    private WorkflowExecutionResult runDirectBot(
            AiBot bot,
            Map<String, Object> executionInput,
            String message,
            List<BotMessage> history
    ) {
        Instant startedAt = Instant.now();
        List<KnowledgeSearchResult> documents = isBlank(bot.knowledgeBaseId())
                ? List.of()
                : knowledgeBaseService.search(bot.knowledgeBaseId(), message, 5);
        String answer;
        if (!isBlank(bot.modelProviderId())) {
            ModelProvider provider = modelProviderService.get(bot.modelProviderId());
            if (!provider.enabled()) {
                throw new IllegalArgumentException("Model provider is disabled: " + bot.modelProviderId());
            }
            answer = chatModelClient.generate(provider.id(), provider.model(), directPrompt(bot, message, history, documents), Map.of(
                    "botId", bot.id(),
                    "knowledgeBaseId", bot.knowledgeBaseId() == null ? "" : bot.knowledgeBaseId()
            ));
        } else {
            answer = documents.isEmpty()
                    ? "No relevant knowledge base content was found."
                    : formatKnowledgeAnswer(documents);
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("answer", answer);
        output.put("documents", documents);
        output.put("mode", "DIRECT_BOT");
        return new WorkflowExecutionResult(new WorkflowExecution(
                "bot_run_" + UUID.randomUUID(),
                "bot:" + bot.id(),
                null,
                WorkflowExecutionStatus.SUCCEEDED,
                executionInput,
                output,
                null,
                startedAt,
                Instant.now()
        ), List.of());
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

    private String directPrompt(
            AiBot bot,
            String message,
            List<BotMessage> history,
            List<KnowledgeSearchResult> documents
    ) {
        List<String> sections = new ArrayList<>();
        if (!isBlank(bot.systemPrompt())) {
            sections.add("System prompt:\n" + bot.systemPrompt());
        }
        if (!history.isEmpty()) {
            sections.add("Conversation history:\n" + history.stream()
                    .map(item -> item.role().name() + ": " + item.content())
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse(""));
        }
        if (!documents.isEmpty()) {
            sections.add("Knowledge base content:\n" + formatKnowledgeAnswer(documents));
        }
        sections.add("User question:\n" + defaultString(message, ""));
        return String.join("\n\n", sections);
    }

    private String formatKnowledgeAnswer(List<KnowledgeSearchResult> documents) {
        return documents.stream()
                .map(item -> "- " + item.documentName() + ": " + item.content())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
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
        String value = defaultString(message, "New chat").trim();
        return value.length() > 30 ? value.substring(0, 30) : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
