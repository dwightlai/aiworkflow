package com.mw.ai.agi.bot.service;

import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotChatResult;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotMessageRole;
import com.mw.ai.agi.chat.service.ChatSseEventSupport;
import com.mw.ai.agi.bot.domain.BotRunResult;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.asset.service.AssetGrantService;
import com.mw.ai.agi.common.asset.AssetReferenceSupport;
import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
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
import com.mw.ai.agi.workflow.engine.WorkflowStreamCitationSupport;
import com.mw.ai.agi.workflow.engine.WorkflowStreamContext;
import com.mw.ai.agi.workflow.engine.WorkflowStreamSink;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BotService {
    private final BotStore store;
    private final WorkflowApplicationService workflowService;
    private final WorkflowExecutionService executionService;
    private final ModelProviderService modelProviderService;
    private final ChatModelClient chatModelClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final AssetGrantService assetGrantService;
    private final TenantBusinessGuard tenantGuard;
    private final BotCapabilityService botCapabilityService;
    private final BotWorkflowRouter botWorkflowRouter;

    public BotService(
            BotStore store,
            WorkflowApplicationService workflowService,
            WorkflowExecutionService executionService,
            ModelProviderService modelProviderService,
            ChatModelClient chatModelClient,
            KnowledgeBaseService knowledgeBaseService,
            AssetGrantService assetGrantService,
            TenantBusinessGuard tenantGuard,
            BotCapabilityService botCapabilityService,
            BotWorkflowRouter botWorkflowRouter
    ) {
        this.store = store;
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.modelProviderService = modelProviderService;
        this.chatModelClient = chatModelClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.assetGrantService = assetGrantService;
        this.tenantGuard = tenantGuard;
        this.botCapabilityService = botCapabilityService;
        this.botWorkflowRouter = botWorkflowRouter;
    }

    public List<AiBot> list(Map<String, Object> context) {
        return store.list(listTenantId()).stream()
                .filter(bot -> assetGrantService.isAllowed(
                        AssetGrantService.BOT,
                        bot.id(),
                        bot.ownerUnitId(),
                        context
                ))
                .toList();
    }

    public List<AiBot> list() {
        return list(Map.of());
    }

    public AiBot create(
            String name,
            String description,
            String ownerUnitId,
            String avatar,
            String workflowId,
            String modelProviderId,
            List<String> knowledgeBaseIds,
            String systemPrompt,
            String openingMessage,
            String capabilityHint,
            List<String> suggestedQuestions,
            BotStatus status
    ) {
        List<String> effectiveKnowledgeBaseIds = AssetReferenceSupport.requireAssetIds(knowledgeBaseIds);
        ensureRunnable(workflowId, modelProviderId, effectiveKnowledgeBaseIds);
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        BotStatus effectiveStatus = status == null ? BotStatus.ENABLED : status;
        AiBot saved = store.save(new AiBot(
                "bot_" + UUID.randomUUID(),
                currentTenantId(),
                name,
                description,
                blankToNull(ownerUnitId),
                defaultString(avatar, "robot"),
                blankToNull(workflowId),
                blankToNull(modelProviderId),
                effectiveKnowledgeBaseIds,
                defaultString(systemPrompt, ""),
                defaultString(openingMessage, ""),
                blankToNull(capabilityHint),
                normalizeSuggestedQuestions(suggestedQuestions),
                effectiveStatus,
                0,
                effectiveStatus == BotStatus.ENABLED ? now : null,
                operator,
                operator,
                now,
                now
        ));
        grantOwner(saved);
        return saved;
    }

    public AiBot update(
            String id,
            String name,
            String description,
            String ownerUnitId,
            String avatar,
            String workflowId,
            String modelProviderId,
            List<String> knowledgeBaseIds,
            String systemPrompt,
            String openingMessage,
            String capabilityHint,
            List<String> suggestedQuestions,
            BotStatus status
    ) {
        AiBot current = get(id);
        List<String> effectiveKnowledgeBaseIds = AssetReferenceSupport.requireAssetIds(knowledgeBaseIds);
        ensureRunnable(workflowId, modelProviderId, effectiveKnowledgeBaseIds);
        BotStatus effectiveStatus = status == null ? current.status() : status;
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        AiBot saved = store.save(new AiBot(
                current.id(),
                current.tenantId(),
                defaultString(name, current.name()),
                description,
                blankToNull(ownerUnitId != null && !ownerUnitId.isBlank() ? ownerUnitId : current.ownerUnitId()),
                defaultString(avatar, current.avatar()),
                blankToNull(workflowId),
                blankToNull(modelProviderId),
                effectiveKnowledgeBaseIds,
                defaultString(systemPrompt, ""),
                defaultString(openingMessage, ""),
                capabilityHint != null ? blankToNull(capabilityHint) : current.capabilityHint(),
                suggestedQuestions != null ? normalizeSuggestedQuestions(suggestedQuestions) : current.suggestedQuestions(),
                effectiveStatus,
                current.conversationCount(),
                effectiveStatus == BotStatus.ENABLED && current.publishedAt() == null ? now : current.publishedAt(),
                current.createdBy(),
                operator,
                current.createdAt(),
                now
        ));
        grantOwner(saved);
        return saved;
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
        assetGrantService.assertAllowed(AssetGrantService.BOT, id, bot.ownerUnitId(), input);
        Map<String, Object> executionInput = executionInput(bot, defaultString(message, ""), input, List.of());
        WorkflowExecutionResult execution = runBotLogic(bot, executionInput, defaultString(message, ""), List.of());
        AiBot updated = store.save(new AiBot(
                bot.id(),
                bot.tenantId(),
                bot.name(),
                bot.description(),
                bot.ownerUnitId(),
                bot.avatar(),
                bot.workflowId(),
                bot.modelProviderId(),
                bot.knowledgeBaseIds(),
                bot.systemPrompt(),
                bot.openingMessage(),
                bot.capabilityHint(),
                bot.suggestedQuestions(),
                bot.status(),
                bot.conversationCount() + 1,
                bot.publishedAt(),
                bot.createdBy(),
                bot.updatedBy(),
                bot.createdAt(),
                Instant.now()
        ));
        return new BotRunResult(updated, execution);
    }

    public BotMessage saveWorkflowReply(String botId, String sessionId, WorkflowExecutionResult execution) {
        get(botId);
        BotSession session = ensureSession(botId, sessionId);
        String content = replyText(execution.execution().output());
        Instant now = Instant.now();
        BotMessage message = store.saveMessage(new BotMessage(
                "msg_" + UUID.randomUUID(),
                session.id(),
                botId,
                BotMessageRole.ASSISTANT,
                content,
                now,
                Map.of("workflowRunId", execution.execution().id()),
                "TEXT"
        ));
        List<BotMessage> messages = store.listMessages(botId, session.id());
        store.saveSession(new BotSession(
                session.id(),
                session.botId(),
                session.title(),
                messages.size(),
                session.createdAt(),
                now,
                session.pinned(),
                session.userId()
        ));
        return message;
    }

    public List<BotSession> listSessions(String botId) {
        get(botId);
        return store.listSessions(botId).stream()
                .map(session -> ensureSessionTitle(botId, session))
                .toList();
    }

    public List<BotSession> listSessionsForUser(String botId, String userId) {
        get(botId);
        return store.listSessions(botId).stream()
                .filter(session -> session.userId() == null || userId == null || userId.equals(session.userId()))
                .map(session -> ensureSessionTitle(botId, session))
                .toList();
    }

    public BotSession createSession(String botId, String title) {
        return createSession(botId, title, null);
    }

    public BotSession createSession(String botId, String title, String userId) {
        AiBot bot = get(botId);
        if (bot.status() != BotStatus.ENABLED) {
            throw new IllegalStateException("Bot is disabled: " + botId);
        }
        Instant now = Instant.now();
        return store.saveSession(new BotSession(
                "session_" + UUID.randomUUID(),
                bot.id(),
                title == null || title.isBlank() ? "新对话" : title,
                0,
                now,
                now,
                false,
                blankToNull(userId)
        ));
    }

    public BotSession ensureSessionForUser(String botId, String sessionId, String userId) {
        BotSession session = ensureSession(botId, sessionId);
        if (session.userId() == null && userId != null && !userId.isBlank()) {
            return store.saveSession(new BotSession(
                    session.id(),
                    session.botId(),
                    session.title(),
                    session.messageCount(),
                    session.createdAt(),
                    Instant.now(),
                    session.pinned(),
                    userId
            ));
        }
        if (session.userId() != null && userId != null && !userId.isBlank() && !session.userId().equals(userId)) {
            throw new IllegalArgumentException("Session not accessible");
        }
        return session;
    }

    public BotSession updateSession(String botId, String sessionId, String title, Boolean pinned) {
        get(botId);
        BotSession session = ensureSession(botId, sessionId);
        String nextTitle = title == null || title.isBlank() ? session.title() : title.trim();
        boolean nextPinned = pinned == null ? session.pinned() : pinned;
        return store.saveSession(new BotSession(
                session.id(),
                session.botId(),
                nextTitle,
                session.messageCount(),
                session.createdAt(),
                Instant.now(),
                nextPinned,
                session.userId()
        ));
    }

    public void deleteSession(String botId, String sessionId) {
        get(botId);
        ensureSession(botId, sessionId);
        store.deleteSession(botId, sessionId);
    }

    public List<BotMessage> listMessages(String botId, String sessionId) {
        get(botId);
        ensureSession(botId, sessionId);
        return store.listMessages(botId, sessionId);
    }

    public List<com.mw.ai.agi.bot.domain.BotCapability> listCapabilities(String botId) {
        get(botId);
        return botCapabilityService.listByBot(botId);
    }

    public com.mw.ai.agi.bot.domain.BotCapability createCapability(
            String botId,
            String capabilityType,
            String capabilityId,
            String capabilityCode,
            String routingKeywords,
            boolean primaryCapability,
            boolean enabled
    ) {
        get(botId);
        workflowService.getWorkflow(capabilityId);
        return botCapabilityService.save(botId, capabilityType, capabilityId, capabilityCode, routingKeywords, primaryCapability, enabled);
    }

    public com.mw.ai.agi.bot.domain.BotCapability updateCapability(
            String botId,
            String capabilityId,
            String capabilityType,
            String workflowId,
            String capabilityCode,
            String routingKeywords,
            Boolean primaryCapability,
            Boolean enabled
    ) {
        get(botId);
        if (workflowId != null && !workflowId.isBlank()) {
            workflowService.getWorkflow(workflowId);
        }
        return botCapabilityService.update(botId, capabilityId, capabilityType, workflowId, capabilityCode, routingKeywords, primaryCapability, enabled);
    }

    public void deleteCapability(String botId, String capabilityId) {
        get(botId);
        botCapabilityService.delete(botId, capabilityId);
    }

    public BotChatResult chat(String botId, String sessionId, String message, Map<String, Object> input) {
        AiBot bot = get(botId);
        if (bot.status() != BotStatus.ENABLED) {
            throw new IllegalStateException("Bot is disabled: " + botId);
        }
        assetGrantService.assertAllowed(AssetGrantService.BOT, botId, bot.ownerUnitId(), input);
        Instant now = Instant.now();
        String userId = blankToNull(input.get("userId") == null ? null : String.valueOf(input.get("userId")));
        BotSession session = sessionId == null || sessionId.isBlank()
                ? store.saveSession(new BotSession(
                        "session_" + UUID.randomUUID(),
                        bot.id(),
                        title(message),
                        0,
                        now,
                        now,
                        false,
                        blankToNull(userId)
                ))
                : ensureSessionForUser(bot.id(), sessionId, userId);

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
        saveCitationMessages(session, bot, execution, userMessageCreatedAt);
        saveGenerationJobMessage(session, bot, execution, userMessageCreatedAt);
        Map<String, Object> assistantMetadata = Map.of();
        String messageType = "TEXT";
        String assistantContent = replyText(execution.execution().output());
        if (execution.execution().status() == WorkflowExecutionStatus.WAITING_CONFIRM) {
            messageType = "CONFIRM";
            assistantMetadata = new LinkedHashMap<>(execution.execution().output());
            assistantContent = String.valueOf(execution.execution().output().getOrDefault("confirmSummary", "需要确认"));
        }
        Instant assistantCreatedAt = userMessageCreatedAt.plusMillis(1000L);
        BotMessage assistantMessage = store.saveMessage(new BotMessage(
                "msg_" + UUID.randomUUID(),
                session.id(),
                bot.id(),
                BotMessageRole.ASSISTANT,
                assistantContent,
                assistantCreatedAt,
                assistantMetadata,
                messageType
        ));
        List<BotMessage> messages = store.listMessages(bot.id(), session.id());
        String sessionTitle = history.isEmpty() ? title(message) : session.title();
        BotSession updatedSession = store.saveSession(new BotSession(
                session.id(),
                session.botId(),
                sessionTitle,
                messages.size(),
                session.createdAt(),
                Instant.now(),
                session.pinned(),
                session.userId()
        ));
        store.save(new AiBot(
                bot.id(),
                bot.tenantId(),
                bot.name(),
                bot.description(),
                bot.ownerUnitId(),
                bot.avatar(),
                bot.workflowId(),
                bot.modelProviderId(),
                bot.knowledgeBaseIds(),
                bot.systemPrompt(),
                bot.openingMessage(),
                bot.capabilityHint(),
                bot.suggestedQuestions(),
                bot.status(),
                bot.conversationCount() + 1,
                bot.publishedAt(),
                bot.createdBy(),
                bot.updatedBy(),
                bot.createdAt(),
                Instant.now()
        ));
        return new BotChatResult(updatedSession, messages, assistantMessage, execution);
    }

    public BotChatResult streamChat(
            String botId,
            String sessionId,
            String message,
            Map<String, Object> input,
            WorkflowStreamSink sink
    ) {
        return WorkflowStreamContext.callWith(sink, () -> chat(botId, sessionId, message, input));
    }

    private List<String> normalizeSuggestedQuestions(List<String> questions) {
        if (questions == null) {
            return List.of();
        }
        return questions.stream()
                .filter(q -> q != null && !q.isBlank())
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
    }

    private AiBot get(String id) {
        AiBot bot = store.findById(id).orElseThrow(() -> new BotNotFoundException(id));
        assertTenantAccessible(bot.tenantId());
        return bot;
    }

    private String currentTenantId() {
        return tenantGuard == null ? TenantContext.requireTenantId() : tenantGuard.currentTenantId();
    }

    private String listTenantId() {
        return currentTenantId();
    }

    private void assertTenantAccessible(String resourceTenantId) {
        if (tenantGuard != null) {
            tenantGuard.assertAccessible(resourceTenantId);
        }
    }

    private void grantOwner(AiBot bot) {
        if (bot.ownerUnitId() == null || bot.ownerUnitId().isBlank()) {
            return;
        }
        assetGrantService.save(AssetGrantService.BOT, bot.id(), AssetGrantService.USE, bot.ownerUnitId(), AssetGrantService.SELF, null, AssetGrantService.SELF, true, null);
    }

    private void ensureRunnable(String workflowId, String modelProviderId, List<String> knowledgeBaseIds) {
        if (!isBlank(workflowId)) {
            workflowService.getWorkflow(workflowId);
            return;
        }
        if (isBlank(modelProviderId) && knowledgeBaseIds.isEmpty()) {
            throw new IllegalArgumentException("Bot requires a workflow, model provider, or knowledge base");
        }
        if (!isBlank(modelProviderId)) {
            ModelProvider provider = modelProviderService.get(modelProviderId);
            if (!provider.enabled()) {
                throw new IllegalArgumentException("Model provider is disabled: " + modelProviderId);
            }
        }
        for (String knowledgeBaseId : knowledgeBaseIds) {
            knowledgeBaseService.search(knowledgeBaseId, "__health_check__", 1);
        }
    }

    private WorkflowExecutionResult runBotLogic(
            AiBot bot,
            Map<String, Object> executionInput,
            String message,
            List<BotMessage> history
    ) {
        String workflowId = botWorkflowRouter.resolveWorkflowId(
                bot,
                botCapabilityService.listByBot(bot.id()),
                message
        );
        if (!isBlank(workflowId)) {
            Map<String, Object> systemVariables = new LinkedHashMap<>();
            copyIfPresent(executionInput, systemVariables, "userId");
            copyIfPresent(executionInput, systemVariables, "activeUnitId");
            copyIfPresent(executionInput, systemVariables, "unitIds");
            copyIfPresent(executionInput, systemVariables, "departmentIds");
            copyIfPresent(executionInput, systemVariables, "tenantId");
            copyIfPresent(executionInput, systemVariables, "userToken");
            copyIfPresent(executionInput, systemVariables, "__traceId");
            copyIfPresent(executionInput, systemVariables, "__conversationId");
            return executionService.runWorkflow(new WorkflowExecutionRequest(workflowId, executionInput, systemVariables));
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
        List<KnowledgeSearchResult> documents = bot.knowledgeBaseIds().isEmpty()
                ? List.of()
                : knowledgeBaseService.searchMany(bot.knowledgeBaseIds(), message, 5, executionInput);
        WorkflowStreamCitationSupport.emitKnowledgeResults(documents);
        String answer;
        if (!isBlank(bot.modelProviderId())) {
            ModelProvider provider = modelProviderService.get(bot.modelProviderId());
            if (!provider.enabled()) {
                throw new IllegalArgumentException("Model provider is disabled: " + bot.modelProviderId());
            }
            Map<String, Object> modelOptions = Map.of(
                    "botId", bot.id(),
                    "knowledgeBaseIds", bot.knowledgeBaseIds()
            );
            String prompt = directPrompt(bot, message, history, documents);
            answer = WorkflowStreamContext.current()
                    .map(sink -> chatModelClient.generateStream(
                            provider.id(),
                            provider.model(),
                            prompt,
                            modelOptions,
                            sink::emitLlmDelta
                    ))
                    .orElseGet(() -> chatModelClient.generate(provider.id(), provider.model(), prompt, modelOptions));
        } else {
            answer = documents.isEmpty()
                    ? "No relevant knowledge base content was found."
                    : formatKnowledgeAnswer(documents);
            WorkflowStreamContext.current().ifPresent(sink -> {
                if (answer != null && !answer.isEmpty()) {
                    sink.emitLlmDelta(answer);
                }
            });
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
        if (!bot.knowledgeBaseIds().isEmpty()) {
            executionInput.put("knowledgeBaseIds", bot.knowledgeBaseIds());
        }
        return executionInput;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key) && source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }

    private List<String> normalizeKnowledgeBaseIds(List<String> knowledgeBaseIds) {
        return AssetReferenceSupport.requireAssetIds(knowledgeBaseIds);
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
        sections.add("Answer rules:\n"
                + "1. Only use the knowledge base excerpts below when they are clearly relevant to the user question.\n"
                + "2. If the excerpts are unrelated or insufficient, clearly say the knowledge base has no relevant information and do not fabricate an answer from them.\n"
                + "3. Do not cite or rely on irrelevant excerpts.");
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

    private void saveGenerationJobMessage(
            BotSession session,
            AiBot bot,
            WorkflowExecutionResult execution,
            Instant userMessageCreatedAt
    ) {
        Map<String, Object> output = execution.execution().output();
        String jobId = firstNonBlank(output.get("generationJobId"), output.get("jobId"));
        if (jobId.isBlank()) {
            return;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jobId", jobId);
        String outputId = firstNonBlank(output.get("generationOutputId"), output.get("outputId"));
        if (!outputId.isBlank()) {
            metadata.put("outputId", outputId);
            metadata.put("downloadUrl", "/api/research/outputs/" + outputId + "/docx");
        }
        metadata.put("title", String.valueOf(output.getOrDefault("title", "编研任务已完成")));
        Object progress = output.get("progress");
        if (progress != null) {
            metadata.put("progress", progress);
        }
        Object step = output.get("currentStep");
        if (step != null) {
            metadata.put("currentStep", step);
        }
        store.saveMessage(new BotMessage(
                "msg_" + UUID.randomUUID(),
                session.id(),
                bot.id(),
                BotMessageRole.ASSISTANT,
                String.valueOf(metadata.get("title")),
                userMessageCreatedAt.plusMillis(500L),
                metadata,
                "JOB"
        ));
    }

    private String firstNonBlank(Object left, Object right) {
        if (left != null && !String.valueOf(left).isBlank()) {
            return String.valueOf(left);
        }
        return right == null ? "" : String.valueOf(right);
    }

    private void saveCitationMessages(
            BotSession session,
            AiBot bot,
            WorkflowExecutionResult execution,
            Instant userMessageCreatedAt
    ) {
        List<Map<String, Object>> citations = ChatSseEventSupport.extractCitations(execution);
        for (int index = 0; index < citations.size(); index++) {
            Map<String, Object> citation = citations.get(index);
            String title = String.valueOf(citation.getOrDefault("title", "引用来源"));
            store.saveMessage(new BotMessage(
                    "msg_" + UUID.randomUUID(),
                    session.id(),
                    bot.id(),
                    BotMessageRole.ASSISTANT,
                    title,
                    userMessageCreatedAt.plusMillis(index + 1L),
                    citation,
                    "CITATION"
            ));
        }
    }

    private String replyText(Map<String, Object> output) {
        if (output == null || output.isEmpty()) {
            return "";
        }
        if (Boolean.TRUE.equals(output.get("confirmRejected"))) {
            return "操作已取消。";
        }
        Object answer = output.get("answer");
        if (answer != null && !String.valueOf(answer).isBlank()) {
            return String.valueOf(answer);
        }
        Object content = output.get("content");
        if (content != null && !String.valueOf(content).isBlank()) {
            return String.valueOf(content);
        }
        Object message = output.get("message");
        if (message != null && !String.valueOf(message).isBlank()) {
            return String.valueOf(message);
        }
        Object text = output.get("text");
        if (text != null && !String.valueOf(text).isBlank()) {
            return String.valueOf(text);
        }
        return output.toString();
    }

    private String title(String message) {
        String value = defaultString(message, "New chat").trim();
        return value.length() > 30 ? value.substring(0, 30) : value;
    }

    private boolean isDefaultSessionTitle(String title) {
        if (title == null || title.isBlank()) {
            return true;
        }
        String normalized = title.trim();
        return "新对话".equals(normalized) || "New chat".equalsIgnoreCase(normalized);
    }

    private BotSession ensureSessionTitle(String botId, BotSession session) {
        if (!isDefaultSessionTitle(session.title()) || session.messageCount() <= 0) {
            return session;
        }
        List<BotMessage> messages = store.listMessages(botId, session.id());
        String firstUserMessage = messages.stream()
                .filter(item -> item.role() == BotMessageRole.USER)
                .map(BotMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .findFirst()
                .orElse(null);
        if (firstUserMessage == null) {
            return session;
        }
        String resolvedTitle = title(firstUserMessage);
        if (resolvedTitle.equals(session.title())) {
            return session;
        }
        return store.saveSession(new BotSession(
                session.id(),
                session.botId(),
                resolvedTitle,
                session.messageCount(),
                session.createdAt(),
                session.updatedAt(),
                session.pinned(),
                session.userId()
        ));
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
