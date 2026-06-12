package com.mw.ai.agi.chat.service;

import com.mw.ai.agi.auth.service.RequestIdentity;
import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotChatResult;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.bot.service.BotService;
import com.mw.ai.agi.chat.persistence.HumanConfirmTaskEntity;
import com.mw.ai.agi.workflow.engine.ConfirmRequiredException;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatGatewayService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final BotService botService;
    private final RequestIdentitySupport identitySupport;
    private final AgentAuditService agentAuditService;
    private final HumanConfirmService humanConfirmService;
    private final ObjectMapper objectMapper;

    public ChatGatewayService(
            BotService botService,
            RequestIdentitySupport identitySupport,
            AgentAuditService agentAuditService,
            HumanConfirmService humanConfirmService,
            ObjectMapper objectMapper
    ) {
        this.botService = botService;
        this.identitySupport = identitySupport;
        this.agentAuditService = agentAuditService;
        this.humanConfirmService = humanConfirmService;
        this.objectMapper = objectMapper;
    }

    public List<AiBot> listAccessibleBots(HttpServletRequest request) {
        return botService.list(identitySupport.grantContext(request)).stream()
                .filter(bot -> bot.status() == BotStatus.ENABLED)
                .toList();
    }

    public AiBot getBot(String botId, HttpServletRequest request) {
        Map<String, Object> context = identitySupport.grantContext(request);
        AiBot bot = botService.list(context).stream()
                .filter(item -> item.id().equals(botId) && item.status() == BotStatus.ENABLED)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bot not accessible: " + botId));
        return bot;
    }

    public List<BotSession> listSessions(String botId, HttpServletRequest request) {
        getBot(botId, request);
        return botService.listSessionsForUser(botId, requireUserId(request));
    }

    public BotSession createSession(String botId, String title, HttpServletRequest request) {
        getBot(botId, request);
        return botService.createSession(botId, title, requireUserId(request));
    }

    public void deleteSession(String botId, String sessionId, HttpServletRequest request) {
        getBot(botId, request);
        assertSessionAccess(botId, sessionId, request);
        botService.deleteSession(botId, sessionId);
    }

    public BotSession updateSession(
            String botId,
            String sessionId,
            String title,
            Boolean pinned,
            HttpServletRequest request
    ) {
        getBot(botId, request);
        assertSessionAccess(botId, sessionId, request);
        return botService.updateSession(botId, sessionId, title, pinned);
    }

    public List<BotMessage> listMessages(String botId, String sessionId, HttpServletRequest request) {
        getBot(botId, request);
        assertSessionAccess(botId, sessionId, request);
        return botService.listMessages(botId, sessionId);
    }

    public BotChatResult chat(String botId, String sessionId, String message, HttpServletRequest request) {
        getBot(botId, request);
        String userId = requireUserId(request);
        if (sessionId != null && !sessionId.isBlank()) {
            botService.ensureSessionForUser(botId, sessionId, userId);
        }
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> input = new LinkedHashMap<>(identitySupport.mergeGrantContext(request, Map.of()));
        input.put("__traceId", traceId);
        input.put("__conversationId", sessionId);
        RequestIdentity identity = identitySupport.resolve(request).orElse(null);
        agentAuditService.log(
                identity == null ? null : identity.userId(),
                botId,
                sessionId,
                null,
                null,
                null,
                "CHAT_MESSAGE",
                truncate(message),
                null,
                "STARTED",
                null,
                traceId
        );
        BotChatResult result = botService.chat(botId, sessionId, message, input);
        agentAuditService.log(
                identity == null ? null : identity.userId(),
                botId,
                result.session().id(),
                result.reply().id(),
                null,
                null,
                "CHAT_REPLY",
                truncate(message),
                truncate(result.reply().content()),
                "SUCCESS",
                null,
                traceId
        );
        return result;
    }

    public SseEmitter streamChat(String botId, String sessionId, String message, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> streamChatAsync(emitter, botId, sessionId, message, request));
        return emitter;
    }

    private void streamChatAsync(
            SseEmitter emitter,
            String botId,
            String sessionId,
            String message,
            HttpServletRequest request
    ) {
        try {
            getBot(botId, request);
            String userId = requireUserId(request);
            if (sessionId != null && !sessionId.isBlank()) {
                botService.ensureSessionForUser(botId, sessionId, userId);
            }
            String traceId = UUID.randomUUID().toString();
            Map<String, Object> input = new LinkedHashMap<>(identitySupport.mergeGrantContext(request, Map.of()));
            input.put("__traceId", traceId);
            input.put("__conversationId", sessionId);
            RequestIdentity identity = identitySupport.resolve(request).orElse(null);
            agentAuditService.log(
                    identity == null ? null : identity.userId(),
                    botId,
                    sessionId,
                    null,
                    null,
                    null,
                    "CHAT_MESSAGE",
                    truncate(message),
                    null,
                    "STARTED",
                    null,
                    traceId
            );

            SseWorkflowStreamSink sink = new SseWorkflowStreamSink(emitter);
            BotChatResult result = botService.streamChat(botId, sessionId, message, input, sink);
            WorkflowExecutionResult execution = result.execution();
            ChatSseEventSupport.emitWorkflowEvents(emitter, execution, false);

            String content = result.reply().content() == null ? "" : result.reply().content();
            emitter.send(SseEmitter.event().name("message.completed").data(Map.of(
                    "content", content,
                    "messageId", result.reply().id()
            )));

            agentAuditService.log(
                    identity == null ? null : identity.userId(),
                    botId,
                    result.session().id(),
                    result.reply().id(),
                    null,
                    null,
                    "CHAT_REPLY",
                    truncate(message),
                    truncate(content),
                    "SUCCESS",
                    null,
                    traceId
            );

            if (execution.execution().status() == WorkflowExecutionStatus.WAITING_CONFIRM) {
                Map<String, Object> output = execution.execution().output();
                String taskId = String.valueOf(output.get("confirmTaskId"));
                String summary = String.valueOf(output.getOrDefault("confirmSummary", "需要确认"));
                ChatSseEventSupport.emitConfirmRequired(emitter, taskId, summary, readConfirmPayload(taskId));
            }

            emitter.send(SseEmitter.event().name("done").data(Map.of()));
            emitter.complete();
        } catch (ConfirmRequiredException ex) {
            try {
                ChatSseEventSupport.emitConfirmRequired(
                        emitter,
                        ex.taskId(),
                        ex.summary(),
                        readConfirmPayload(ex.taskId())
                );
                emitter.send(SseEmitter.event().name("done").data(Map.of()));
                emitter.complete();
            } catch (IOException ioEx) {
                emitter.completeWithError(ioEx);
            }
        } catch (Exception ex) {
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", ex.getMessage())));
                emitter.send(SseEmitter.event().name("done").data(Map.of()));
            } catch (IOException ignored) {
            }
            emitter.completeWithError(ex);
        }
    }

    private Map<String, Object> readConfirmPayload(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return Map.of();
        }
        try {
            HumanConfirmTaskEntity task = humanConfirmService.getTask(taskId);
            if (task.getPayloadSnapshot() == null || task.getPayloadSnapshot().isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(task.getPayloadSnapshot(), MAP_TYPE);
        } catch (RuntimeException ex) {
            return Map.of();
        } catch (IOException ex) {
            return Map.of();
        }
    }

    private void assertSessionAccess(String botId, String sessionId, HttpServletRequest request) {
        botService.ensureSessionForUser(botId, sessionId, requireUserId(request));
    }

    private String requireUserId(HttpServletRequest request) {
        return identitySupport.resolve(request)
                .map(RequestIdentity::userId)
                .orElseThrow(() -> new IllegalArgumentException("Authentication required"));
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
