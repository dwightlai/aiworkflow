package com.mw.ai.agi.bot.api;

import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.OpenApiRequestContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotCapability;
import com.mw.ai.agi.bot.domain.BotChatResult;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotRunResult;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.bot.service.BotCapabilityService;
import com.mw.ai.agi.bot.service.BotService;
import com.mw.ai.agi.chat.service.ChatGatewayService;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.workflow.api.WorkflowExecutionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/open/bots")
@Tag(name = "智能体", description = "智能体列表、运行、多轮对话与流式消息")
public class OpenBotController {
    private final BotService botService;
    private final BotCapabilityService botCapabilityService;
    private final ChatGatewayService chatGatewayService;
    private final IntegrationAppScopeService scopeService;

    public OpenBotController(
            BotService botService,
            BotCapabilityService botCapabilityService,
            ChatGatewayService chatGatewayService,
            IntegrationAppScopeService scopeService
    ) {
        this.botService = botService;
        this.botCapabilityService = botCapabilityService;
        this.chatGatewayService = chatGatewayService;
        this.scopeService = scopeService;
    }

    @GetMapping
    @Operation(summary = "列出可调智能体")
    public ApiResponse<PageResponse<OpenBotView>> list(HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        List<OpenBotView> bots = botService.list(Map.of()).stream()
                .filter(bot -> bot.status() == BotStatus.ENABLED)
                .filter(bot -> scopeService.isAssetAllowed(
                        identity.appId(),
                        IntegrationAppScopeService.SCOPE_BOT,
                        bot.id()
                ))
                .map(OpenBotView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(bots, bots.size()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取智能体详情")
    public ApiResponse<OpenBotView> get(@PathVariable String id, HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        return ApiResponse.success(OpenBotView.from(requireEnabledBot(id, identity)));
    }

    @GetMapping("/{id}/capabilities")
    @Operation(summary = "列出智能体多工作流路由能力")
    public ApiResponse<PageResponse<OpenBotCapabilityView>> listCapabilities(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, id);
        requireEnabledBot(id, identity);
        List<OpenBotCapabilityView> items = botCapabilityService.listByBot(id).stream()
                .filter(BotCapability::enabled)
                .map(OpenBotCapabilityView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @GetMapping("/{id}/sessions")
    @Operation(summary = "列出用户会话", description = "需携带 X-AGI-User-Id 或请求体 userId")
    public ApiResponse<PageResponse<BotSession>> listSessions(@PathVariable String id, HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, id);
        requireEnabledBot(id, identity);
        String userId = requireUserId(identity);
        List<BotSession> sessions = botService.listSessionsForUser(id, userId);
        return ApiResponse.success(new PageResponse<>(sessions, sessions.size()));
    }

    @PostMapping("/{id}/sessions")
    @Operation(summary = "创建会话")
    public ApiResponse<BotSession> createSession(
            @PathVariable String id,
            @RequestBody(required = false) OpenBotCreateSessionRequest body,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, id);
        requireEnabledBot(id, identity);
        OpenBotCreateSessionRequest safe = body == null ? new OpenBotCreateSessionRequest("新对话", null) : body;
        RuntimeIdentityContext resolved = resolveIdentity(identity, safe.userId(), null, null, null);
        String title = safe.title() == null || safe.title().isBlank() ? "新对话" : safe.title();
        return ApiResponse.success(botService.createSession(id, title, requireUserId(resolved)));
    }

    @GetMapping("/{id}/sessions/{sessionId}/messages")
    @Operation(summary = "列出会话消息")
    public ApiResponse<PageResponse<BotMessage>> listMessages(
            @PathVariable String id,
            @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, id);
        requireEnabledBot(id, identity);
        botService.ensureSessionForUser(id, sessionId, requireUserId(identity));
        List<BotMessage> messages = botService.listMessages(id, sessionId);
        return ApiResponse.success(new PageResponse<>(messages, messages.size()));
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "单次运行智能体")
    public ApiResponse<OpenBotRunResponse> run(
            @PathVariable String id,
            @RequestBody(required = false) OpenBotRunRequest body,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = resolveIdentity(request, body == null ? null : body.userId(), body == null ? null : body.unitId(),
                body == null ? null : body.departmentIds(), body == null ? null : body.roleIds());
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, id);
        OpenBotRunRequest safeRequest = body == null ? new OpenBotRunRequest(null, null, null, null, null, Map.of()) : body;
        BotRunResult result = botService.run(id, safeRequest.message(), executionInput(identity, safeRequest.input()));
        return ApiResponse.success(new OpenBotRunResponse(OpenBotView.from(result.bot()), WorkflowExecutionResponse.from(result.execution())));
    }

    @PostMapping("/{id}/chat")
    @Operation(summary = "多轮对话（同步）")
    public ApiResponse<OpenBotChatResponse> chat(
            @PathVariable String id,
            @RequestBody(required = false) OpenBotChatRequest body,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = resolveIdentity(request, body == null ? null : body.userId(), body == null ? null : body.unitId(),
                body == null ? null : body.departmentIds(), body == null ? null : body.roleIds());
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, id);
        OpenBotChatRequest safeRequest = body == null ? new OpenBotChatRequest(null, null, null, null, null, null, Map.of()) : body;
        BotChatResult result = botService.chat(
                id,
                safeRequest.sessionId(),
                safeRequest.message(),
                executionInput(identity, safeRequest.input())
        );
        return ApiResponse.success(new OpenBotChatResponse(
                result.session(),
                result.messages(),
                result.reply(),
                WorkflowExecutionResponse.from(result.execution())
        ));
    }

    @PostMapping(value = "/{id}/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "多轮对话（SSE 流式）", description = """
            SSE 事件：message.delta、message.completed、citation.added、tool.completed、confirm.required、job.*、error、done
            """)
    public SseEmitter streamMessage(
            @PathVariable String id,
            @PathVariable String sessionId,
            @RequestBody(required = false) OpenBotStreamRequest body,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = resolveIdentity(request, body == null ? null : body.userId(), body == null ? null : body.unitId(),
                body == null ? null : body.departmentIds(), body == null ? null : body.roleIds());
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, id);
        requireEnabledBot(id, identity);
        OpenBotStreamRequest safe = body == null ? new OpenBotStreamRequest("", null, null, null, null, Map.of()) : body;
        Map<String, Object> input = executionInput(identity, safe.input());
        input.put("__traceId", UUID.randomUUID().toString());
        input.put("__conversationId", sessionId);
        return chatGatewayService.streamChat(
                id,
                sessionId,
                safe.message() == null ? "" : safe.message(),
                input,
                requireUserId(identity)
        );
    }

    private AiBot requireEnabledBot(String id, RuntimeIdentityContext identity) {
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, id);
        AiBot bot = botService.list(Map.of()).stream()
                .filter(item -> id.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Bot not found: " + id
                ));
        if (bot.status() != BotStatus.ENABLED) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Bot not found: " + id
            );
        }
        return bot;
    }

    private RuntimeIdentityContext resolveIdentity(
            HttpServletRequest request,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds
    ) {
        return resolveIdentity(OpenApiRequestContext.require(request), userId, unitId, departmentIds, roleIds);
    }

    private RuntimeIdentityContext resolveIdentity(
            RuntimeIdentityContext identity,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds
    ) {
        if (userId == null && unitId == null && (departmentIds == null || departmentIds.isEmpty()) && (roleIds == null || roleIds.isEmpty())) {
            return identity;
        }
        return new RuntimeIdentityContext(
                identity.tenantId(),
                identity.appId(),
                firstNonBlank(userId, identity.userId()),
                identity.unitIds(),
                firstNonBlank(unitId, identity.activeUnitId()),
                departmentIds == null || departmentIds.isEmpty() ? identity.departmentIds() : departmentIds,
                roleIds == null || roleIds.isEmpty() ? identity.roleIds() : roleIds,
                identity.authType(),
                identity.source()
        );
    }

    private Map<String, Object> executionInput(RuntimeIdentityContext identity, Map<String, Object> input) {
        Map<String, Object> merged = new LinkedHashMap<>(OpenApiRequestContext.grantContext(identity));
        if (input != null) {
            merged.putAll(input);
        }
        return merged;
    }

    private String requireUserId(RuntimeIdentityContext identity) {
        if (identity.userId() == null || identity.userId().isBlank()) {
            throw new IllegalArgumentException("X-AGI-User-Id or userId is required for session operations");
        }
        return identity.userId();
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record OpenBotView(
            String id,
            String name,
            String description,
            String avatar,
            String workflowId,
            String modelProviderId,
            List<String> knowledgeBaseIds,
            String openingMessage,
            String capabilityHint,
            List<String> suggestedQuestions,
            BotStatus status
    ) {
        static OpenBotView from(AiBot bot) {
            return new OpenBotView(
                    bot.id(),
                    bot.name(),
                    bot.description(),
                    bot.avatar(),
                    bot.workflowId(),
                    bot.modelProviderId(),
                    bot.knowledgeBaseIds(),
                    bot.openingMessage(),
                    bot.capabilityHint(),
                    bot.suggestedQuestions(),
                    bot.status()
            );
        }
    }

    public record OpenBotCapabilityView(
            String id,
            String capabilityType,
            String capabilityId,
            String capabilityCode,
            String routingKeywords,
            boolean primaryCapability,
            boolean enabled
    ) {
        static OpenBotCapabilityView from(BotCapability capability) {
            return new OpenBotCapabilityView(
                    capability.id(),
                    capability.capabilityType(),
                    capability.capabilityId(),
                    capability.capabilityCode(),
                    capability.routingKeywords(),
                    capability.primaryCapability(),
                    capability.enabled()
            );
        }
    }

    public record OpenBotRunRequest(
            String message,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds,
            Map<String, Object> input
    ) {
    }

    public record OpenBotChatRequest(
            String sessionId,
            String message,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds,
            Map<String, Object> input
    ) {
    }

    public record OpenBotStreamRequest(
            String message,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds,
            Map<String, Object> input
    ) {
    }

    public record OpenBotCreateSessionRequest(
            String title,
            String userId
    ) {
    }

    public record OpenBotRunResponse(OpenBotView bot, WorkflowExecutionResponse execution) {
    }

    public record OpenBotChatResponse(
            BotSession session,
            List<BotMessage> messages,
            BotMessage reply,
            WorkflowExecutionResponse execution
    ) {
    }
}
