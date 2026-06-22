package com.mw.ai.agi.chat.api;

import com.mw.ai.agi.auth.service.AuthTokenResponse;
import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.bot.service.BotService;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;
import com.mw.ai.agi.chat.persistence.HumanConfirmTaskEntity;
import com.mw.ai.agi.chat.domain.AgentJob;
import com.mw.ai.agi.chat.service.AgentAuditService;
import com.mw.ai.agi.chat.service.AgentJobService;
import com.mw.ai.agi.chat.service.ChatGatewayService;
import com.mw.ai.agi.chat.service.EmbedTicketService;
import com.mw.ai.agi.chat.service.HumanConfirmService;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.mw.ai.agi.generation.api.ResearchApiMapper;
import com.mw.ai.agi.generation.service.ResearchGenerationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatGatewayService chatGatewayService;
    private final HumanConfirmService humanConfirmService;
    private final WorkflowExecutionService workflowExecutionService;
    private final EmbedTicketService embedTicketService;
    private final BotService botService;
    private final RequestIdentitySupport identitySupport;
    private final AgentAuditService agentAuditService;
    private final AgentJobService agentJobService;
    private final ResearchGenerationService researchGenerationService;
    private final ObjectMapper objectMapper;

    public ChatController(
            ChatGatewayService chatGatewayService,
            HumanConfirmService humanConfirmService,
            WorkflowExecutionService workflowExecutionService,
            EmbedTicketService embedTicketService,
            BotService botService,
            RequestIdentitySupport identitySupport,
            AgentAuditService agentAuditService,
            AgentJobService agentJobService,
            ResearchGenerationService researchGenerationService,
            ObjectMapper objectMapper
    ) {
        this.chatGatewayService = chatGatewayService;
        this.humanConfirmService = humanConfirmService;
        this.workflowExecutionService = workflowExecutionService;
        this.embedTicketService = embedTicketService;
        this.botService = botService;
        this.identitySupport = identitySupport;
        this.agentAuditService = agentAuditService;
        this.agentJobService = agentJobService;
        this.researchGenerationService = researchGenerationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/bots")
    public ApiResponse<PageResponse<AiBot>> listBots(HttpServletRequest request) {
        List<AiBot> bots = chatGatewayService.listAccessibleBots(request);
        return ApiResponse.success(new PageResponse<>(bots, bots.size()));
    }

    @GetMapping("/bots/{id}")
    public ApiResponse<AiBot> getBot(@PathVariable String id, HttpServletRequest request) {
        return ApiResponse.success(chatGatewayService.getBot(id, request));
    }

    @GetMapping("/bots/{botId}/sessions")
    public ApiResponse<PageResponse<BotSession>> sessions(@PathVariable String botId, HttpServletRequest request) {
        List<BotSession> sessions = chatGatewayService.listSessions(botId, request);
        return ApiResponse.success(new PageResponse<>(sessions, sessions.size()));
    }

    @PostMapping("/bots/{botId}/sessions")
    public ApiResponse<BotSession> createSession(
            @PathVariable String botId,
            @RequestBody(required = false) CreateSessionRequest body,
            HttpServletRequest request
    ) {
        CreateSessionRequest safe = body == null ? new CreateSessionRequest("新对话") : body;
        return ApiResponse.success(chatGatewayService.createSession(botId, safe.title(), request));
    }

    @DeleteMapping("/bots/{botId}/sessions/{sessionId}")
    public ApiResponse<Map<String, Boolean>> deleteSession(
            @PathVariable String botId,
            @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        chatGatewayService.deleteSession(botId, sessionId, request);
        return ApiResponse.success(Map.of("deleted", true));
    }

    @PatchMapping("/bots/{botId}/sessions/{sessionId}")
    public ApiResponse<BotSession> updateSession(
            @PathVariable String botId,
            @PathVariable String sessionId,
            @RequestBody(required = false) UpdateSessionRequest body,
            HttpServletRequest request
    ) {
        UpdateSessionRequest safe = body == null ? new UpdateSessionRequest(null, null) : body;
        return ApiResponse.success(chatGatewayService.updateSession(
                botId,
                sessionId,
                safe.title(),
                safe.pinned(),
                request
        ));
    }

    @GetMapping("/bots/{botId}/sessions/{sessionId}/messages")
    public ApiResponse<PageResponse<BotMessage>> messages(
            @PathVariable String botId,
            @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        List<BotMessage> messages = chatGatewayService.listMessages(botId, sessionId, request);
        return ApiResponse.success(new PageResponse<>(messages, messages.size()));
    }

    @PostMapping(value = "/bots/{botId}/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @PathVariable String botId,
            @PathVariable String sessionId,
            @RequestBody(required = false) StreamMessageRequest body,
            HttpServletRequest request
    ) {
        StreamMessageRequest safe = body == null ? new StreamMessageRequest("") : body;
        return chatGatewayService.streamChat(botId, sessionId, safe.message(), request);
    }

    @PostMapping("/embed-sessions/exchange")
    public ApiResponse<AuthTokenResponse> exchangeEmbedSession(@Valid @RequestBody ExchangeEmbedSessionRequest body) {
        return ApiResponse.success(embedTicketService.exchange(body.ticket(), body.botId()));
    }

    @GetMapping("/agent-jobs/{jobId}")
    public ApiResponse<AgentJobView> getAgentJob(@PathVariable String jobId, HttpServletRequest request) {
        String userId = requireUserId(request);
        AgentJob job = agentJobService.getForUser(jobId, userId);
        return ApiResponse.success(toAgentJobView(job));
    }

    @GetMapping("/generation-jobs/{jobId}")
    public ApiResponse<ResearchApiMapper.ResearchJobView> getGenerationJob(
            @PathVariable String jobId,
            HttpServletRequest request
    ) {
        requireUserId(request);
        return ApiResponse.success(ResearchApiMapper.toJobView(researchGenerationService.getJob(jobId), objectMapper));
    }

    @GetMapping("/confirm-tasks/{taskId}")
    public ApiResponse<ConfirmTaskView> getConfirmTask(@PathVariable String taskId, HttpServletRequest request) {
        String userId = requireUserId(request);
        HumanConfirmTaskEntity task = humanConfirmService.getTask(taskId);
        if (!userId.equals(task.getUserId())) {
            throw new IllegalArgumentException("Confirm task not accessible");
        }
        return ApiResponse.success(toConfirmTaskView(task));
    }

    @PostMapping("/confirm-tasks/{taskId}/confirm")
    public ApiResponse<ConfirmTaskResponse> confirm(@PathVariable String taskId, HttpServletRequest request) {
        String userId = requireUserId(request);
        HumanConfirmTaskEntity task = humanConfirmService.confirm(taskId, userId);
        WorkflowExecutionResult execution = workflowExecutionService.resumeAfterConfirm(task.getWorkflowRunId(), taskId);
        BotMessage reply = botService.saveWorkflowReply(task.getBotId(), task.getConversationId(), execution);
        agentAuditService.log(userId, task.getBotId(), task.getConversationId(), reply.id(), task.getConnectorCode(),
                task.getOperationCode(), "HITL_CONFIRM", task.getSummary(), truncate(reply.content()), "CONFIRMED", null, resolveTraceId(task));
        return ApiResponse.success(new ConfirmTaskResponse(task.getId(), "CONFIRMED", reply));
    }

    @PostMapping("/confirm-tasks/{taskId}/reject")
    public ApiResponse<ConfirmTaskResponse> reject(@PathVariable String taskId, HttpServletRequest request) {
        String userId = requireUserId(request);
        HumanConfirmTaskEntity task = humanConfirmService.reject(taskId, userId);
        WorkflowExecutionResult execution = workflowExecutionService.rejectAfterConfirm(task.getWorkflowRunId(), taskId);
        BotMessage reply = botService.saveWorkflowReply(task.getBotId(), task.getConversationId(), execution);
        agentAuditService.log(userId, task.getBotId(), task.getConversationId(), reply.id(), task.getConnectorCode(),
                task.getOperationCode(), "HITL_REJECT", task.getSummary(), truncate(reply.content()), "REJECTED", null, resolveTraceId(task));
        return ApiResponse.success(new ConfirmTaskResponse(task.getId(), "REJECTED", reply));
    }

    private String requireUserId(HttpServletRequest request) {
        return identitySupport.resolve(request).map(com.mw.ai.agi.auth.service.RequestIdentity::userId)
                .orElseThrow(() -> new IllegalArgumentException("Authentication required"));
    }

    private ConfirmTaskView toConfirmTaskView(HumanConfirmTaskEntity task) {
        return new ConfirmTaskView(
                task.getId(),
                task.getStatus(),
                task.getSummary(),
                readPayload(task.getPayloadSnapshot()),
                task.getConnectorCode(),
                task.getOperationCode()
        );
    }

    private Map<String, Object> readPayload(String payloadSnapshot) {
        if (payloadSnapshot == null || payloadSnapshot.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadSnapshot, new TypeReference<>() {
            });
        } catch (IOException ex) {
            return Map.of("raw", payloadSnapshot);
        }
    }

    private String resolveTraceId(HumanConfirmTaskEntity task) {
        if (task.getWorkflowRunId() == null || task.getWorkflowRunId().isBlank()) {
            return null;
        }
        try {
            WorkflowExecutionResult execution = workflowExecutionService.getWorkflowExecution(task.getWorkflowRunId());
            Object traceId = execution.execution().context().get("__traceId");
            if (traceId == null) {
                traceId = execution.execution().input().get("__traceId");
            }
            if (traceId != null && !String.valueOf(traceId).isBlank()) {
                return String.valueOf(traceId);
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private AgentJobView toAgentJobView(AgentJob job) {
        String downloadUrl = null;
        String resultUrl = null;
        String title = null;
        if (job.result() != null && !job.result().isBlank()) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(job.result(), new TypeReference<>() {
                });
                downloadUrl = stringOrNull(parsed.get("downloadUrl"));
                resultUrl = stringOrNull(parsed.get("resultUrl"));
                title = stringOrNull(parsed.get("title"));
            } catch (IOException ignored) {
            }
        }
        return new AgentJobView(
                job.id(),
                job.status(),
                job.progress(),
                job.currentStep(),
                job.errorMessage(),
                job.result(),
                downloadUrl,
                resultUrl,
                title
        );
    }

    private String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record StreamMessageRequest(String message) {
    }

    public record CreateSessionRequest(String title) {
    }

    public record UpdateSessionRequest(String title, Boolean pinned) {
    }

    public record ExchangeEmbedSessionRequest(@NotBlank String ticket, @NotBlank String botId) {
    }

    public record ConfirmTaskResponse(String taskId, String status, BotMessage reply) {
    }

    public record ConfirmTaskView(
            String taskId,
            String status,
            String summary,
            Map<String, Object> payloadSnapshot,
            String connectorCode,
            String operationCode
    ) {
    }

    public record AgentJobView(
            String id,
            String status,
            Integer progress,
            String currentStep,
            String errorMessage,
            String result,
            String downloadUrl,
            String resultUrl,
            String title
    ) {
    }
}
