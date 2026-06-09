package com.mw.ai.agi.bot.api;

import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.OpenApiRequestContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotChatResult;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotRunResult;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.bot.service.BotService;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.workflow.api.WorkflowExecutionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/open/bots")
public class OpenBotController {
    private final BotService botService;
    private final IntegrationAppScopeService scopeService;

    public OpenBotController(BotService botService, IntegrationAppScopeService scopeService) {
        this.botService = botService;
        this.scopeService = scopeService;
    }

    @GetMapping
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
    public ApiResponse<OpenBotView> get(@PathVariable String id, HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
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
        return ApiResponse.success(OpenBotView.from(bot));
    }

    @PostMapping("/{id}/run")
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

    private RuntimeIdentityContext resolveIdentity(
            HttpServletRequest request,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds
    ) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
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
        Map<String, Object> merged = new LinkedHashMap<>(OpenApiRequestContext.auditContext(identity));
        if (input != null) {
            merged.putAll(input);
        }
        return merged;
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
            List<String> knowledgeBaseIds,
            String openingMessage,
            BotStatus status
    ) {
        static OpenBotView from(AiBot bot) {
            return new OpenBotView(
                    bot.id(),
                    bot.name(),
                    bot.description(),
                    bot.avatar(),
                    bot.workflowId(),
                    bot.knowledgeBaseIds(),
                    bot.openingMessage(),
                    bot.status()
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
