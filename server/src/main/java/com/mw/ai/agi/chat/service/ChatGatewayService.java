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
import com.mw.ai.agi.common.trace.TraceIdSupport;
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
    private final AgentJobService agentJobService;

    public ChatGatewayService(
            BotService botService,
            RequestIdentitySupport identitySupport,
            AgentAuditService agentAuditService,
            HumanConfirmService humanConfirmService,
            ObjectMapper objectMapper,
            AgentJobService agentJobService
    ) {
        this.botService = botService;
        this.identitySupport = identitySupport;
        this.agentAuditService = agentAuditService;
        this.humanConfirmService = humanConfirmService;
        this.objectMapper = objectMapper;
        this.agentJobService = agentJobService;
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
        String traceId = TraceIdSupport.resolve(request);
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
        logWorkflowRun(
                identity == null ? null : identity.userId(),
                botId,
                result.session().id(),
                result.reply().id(),
                result.execution(),
                traceId
        );
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
        getBot(botId, request);
        String userId = requireUserId(request);
        Map<String, Object> input = buildStreamInput(request, sessionId);
        return streamChat(botId, sessionId, message, input, userId);
    }

    public SseEmitter streamChat(
            String botId,
            String sessionId,
            String message,
            Map<String, Object> input,
            String userId
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> streamChatCore(emitter, botId, sessionId, message, input, userId));
        return emitter;
    }

    private Map<String, Object> buildStreamInput(HttpServletRequest request, String sessionId) {
        String traceId = TraceIdSupport.resolve(request);
        Map<String, Object> input = new LinkedHashMap<>(identitySupport.mergeGrantContext(request, Map.of()));
        input.put("__traceId", traceId);
        input.put("__conversationId", sessionId);
        return input;
    }

    private void streamChatCore(
            SseEmitter emitter,
            String botId,
            String sessionId,
            String message,
            Map<String, Object> input,
            String userId
    ) {
        try {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("Authentication required");
            }
            if (sessionId != null && !sessionId.isBlank()) {
                botService.ensureSessionForUser(botId, sessionId, userId);
            }
            String traceId = String.valueOf(input.getOrDefault("__traceId", UUID.randomUUID().toString()));
            agentAuditService.log(
                    userId,
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

            var routePreview = botService.previewWorkflowRoute(botId, message);
            if (routePreview.workflowId() != null && !routePreview.workflowId().isBlank()) {
                Map<String, Object> routeData = new LinkedHashMap<>();
                routeData.put("workflowId", routePreview.workflowId());
                routeData.put("workflowName", routePreview.workflowName());
                routeData.put("matchReason", routePreview.matchReason());
                if (routePreview.capabilityCode() != null && !routePreview.capabilityCode().isBlank()) {
                    routeData.put("capabilityCode", routePreview.capabilityCode());
                }
                emitter.send(SseEmitter.event().name("route.selected").data(routeData));
            }

            SseWorkflowStreamSink sink = new SseWorkflowStreamSink(emitter);
            BotChatResult result = botService.streamChat(botId, sessionId, message, input, sink);
            WorkflowExecutionResult execution = result.execution();
            ChatSseEventSupport.emitWorkflowEvents(emitter, execution, false, true);
            logWorkflowRun(userId, botId, result.session().id(), result.reply().id(), execution, traceId);
            agentJobService.upsertFromOutput(userId, botId, result.session().id(), execution.execution().output());

            String content = result.reply().content() == null ? "" : result.reply().content();
            emitter.send(SseEmitter.event().name("message.completed").data(Map.of(
                    "content", content,
                    "messageId", result.reply().id()
            )));

            agentAuditService.log(
                    userId,
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
            String traceId = String.valueOf(input.getOrDefault("__traceId", ""));
            agentAuditService.log(
                    userId,
                    botId,
                    sessionId,
                    null,
                    null,
                    null,
                    "CHAT_STREAM_FAILED",
                    truncate(message),
                    null,
                    "FAILED",
                    ex.getMessage(),
                    traceId.isBlank() ? null : traceId
            );
            try {
                String clientMessage = resolveStreamErrorMessage(ex);
                emitter.send(SseEmitter.event().name("error").data(Map.of(
                        "message", clientMessage,
                        "code", resolveStreamErrorCode(ex)
                )));
                emitter.send(SseEmitter.event().name("done").data(Map.of()));
            } catch (IOException ignored) {
            }
            emitter.completeWithError(ex);
        }
    }

    private String resolveStreamErrorCode(Exception ex) {
        if (ex instanceof IllegalArgumentException && ex.getMessage() != null && ex.getMessage().contains("Authentication")) {
            return "CHAT_AUTH_REQUIRED";
        }
        if (ex instanceof IllegalArgumentException && ex.getMessage() != null && ex.getMessage().contains("accessible")) {
            return "CHAT_FORBIDDEN";
        }
        return "CHAT_STREAM_ERROR";
    }

    private String resolveStreamErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "对话处理失败，请稍后重试";
        }
        if (message.contains("Authentication required")) {
            return "登录已失效，请重新登录";
        }
        if (message.contains("timed out") || message.contains("Timeout")) {
            return "请求超时，请稍后重试";
        }
        return message;
    }

    private void logWorkflowRun(
            String userId,
            String botId,
            String conversationId,
            String messageId,
            WorkflowExecutionResult execution,
            String traceId
    ) {
        if (execution == null || execution.execution() == null) {
            return;
        }
        var run = execution.execution();
        String status = run.status() == WorkflowExecutionStatus.FAILED ? "FAILED" : "SUCCESS";
        agentAuditService.log(
                userId,
                botId,
                conversationId,
                messageId,
                run.workflowId(),
                null,
                "WORKFLOW_RUN",
                run.id(),
                run.status().name(),
                status,
                run.errorMessage(),
                traceId
        );
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
