package com.mw.ai.agi.bot.api;

import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotChatResult;
import com.mw.ai.agi.bot.domain.BotCapability;
import com.mw.ai.agi.bot.domain.WorkflowRoutePreview;
import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotRunResult;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.bot.service.BotService;
import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.workflow.api.WorkflowExecutionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bots")
public class BotController {
    private final BotService botService;
    private final RequestIdentitySupport identitySupport;

    public BotController(BotService botService, RequestIdentitySupport identitySupport) {
        this.botService = botService;
        this.identitySupport = identitySupport;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiBot>> list(HttpServletRequest request) {
        List<AiBot> bots = botService.list(identitySupport.grantContext(request));
        return ApiResponse.success(new PageResponse<>(bots, bots.size()));
    }

    @PostMapping
    public ApiResponse<AiBot> create(@Valid @RequestBody SaveBotRequest body, HttpServletRequest request) {
        return ApiResponse.success(botService.create(
                body.name(),
                body.description(),
                identitySupport.resolveOwnerUnitId(body.ownerUnitId(), request),
                body.avatar(),
                body.workflowId(),
                body.modelProviderId(),
                body.knowledgeBaseIds(),
                body.systemPrompt(),
                body.openingMessage(),
                body.capabilityHint(),
                body.suggestedQuestions(),
                body.status()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiBot> update(@PathVariable String id, @Valid @RequestBody SaveBotRequest body) {
        return ApiResponse.success(botService.update(
                id,
                body.name(),
                body.description(),
                body.ownerUnitId(),
                body.avatar(),
                body.workflowId(),
                body.modelProviderId(),
                body.knowledgeBaseIds(),
                body.systemPrompt(),
                body.openingMessage(),
                body.capabilityHint(),
                body.suggestedQuestions(),
                body.status()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        botService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/run")
    public ApiResponse<BotRunResponse> run(
            @PathVariable String id,
            @RequestBody(required = false) RunBotRequest body,
            HttpServletRequest request
    ) {
        RunBotRequest safeRequest = body == null ? new RunBotRequest("", Map.of()) : body;
        BotRunResult result = botService.run(
                id,
                safeRequest.message(),
                identitySupport.mergeGrantContext(request, safeInput(safeRequest.input()))
        );
        return ApiResponse.success(new BotRunResponse(result.bot(), WorkflowExecutionResponse.from(result.execution())));
    }

    @GetMapping("/{id}/sessions")
    public ApiResponse<PageResponse<BotSession>> sessions(@PathVariable String id) {
        List<BotSession> sessions = botService.listSessions(id);
        return ApiResponse.success(new PageResponse<>(sessions, sessions.size()));
    }

    @GetMapping("/{id}/sessions/{sessionId}/messages")
    public ApiResponse<PageResponse<BotMessage>> messages(@PathVariable String id, @PathVariable String sessionId) {
        List<BotMessage> messages = botService.listMessages(id, sessionId);
        return ApiResponse.success(new PageResponse<>(messages, messages.size()));
    }

    @PostMapping("/{id}/chat")
    public ApiResponse<BotChatResponse> chat(
            @PathVariable String id,
            @RequestBody(required = false) ChatBotRequest body,
            HttpServletRequest request
    ) {
        ChatBotRequest safeRequest = body == null ? new ChatBotRequest(null, "", Map.of()) : body;
        BotChatResult result = botService.chat(
                id,
                safeRequest.sessionId(),
                safeRequest.message(),
                identitySupport.mergeGrantContext(request, safeInput(safeRequest.input()))
        );
        return ApiResponse.success(new BotChatResponse(
                result.session(),
                result.messages(),
                result.reply(),
                WorkflowExecutionResponse.from(result.execution())
        ));
    }

    @GetMapping("/{id}/capabilities")
    public ApiResponse<PageResponse<BotCapability>> capabilities(@PathVariable String id) {
        List<BotCapability> items = botService.listCapabilities(id);
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @PostMapping("/{id}/capabilities")
    public ApiResponse<BotCapability> createCapability(@PathVariable String id, @RequestBody SaveBotCapabilityRequest body) {
        return ApiResponse.success(botService.createCapability(
                id,
                body.capabilityType() == null ? "WORKFLOW" : body.capabilityType(),
                body.capabilityId(),
                body.capabilityCode(),
                body.routingKeywords(),
                Boolean.TRUE.equals(body.primaryCapability()),
                body.enabled() == null || body.enabled()
        ));
    }

    @PutMapping("/{id}/capabilities/{capabilityId}")
    public ApiResponse<BotCapability> updateCapability(
            @PathVariable String id,
            @PathVariable String capabilityId,
            @RequestBody SaveBotCapabilityRequest body
    ) {
        return ApiResponse.success(botService.updateCapability(
                id,
                capabilityId,
                body.capabilityType(),
                body.capabilityId(),
                body.capabilityCode(),
                body.routingKeywords(),
                body.primaryCapability(),
                body.enabled()
        ));
    }

    @DeleteMapping("/{id}/capabilities/{capabilityId}")
    public ApiResponse<Void> deleteCapability(@PathVariable String id, @PathVariable String capabilityId) {
        botService.deleteCapability(id, capabilityId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/capabilities/route-preview")
    public ApiResponse<WorkflowRoutePreview> previewCapabilityRoute(
            @PathVariable String id,
            @RequestBody RoutePreviewRequest body
    ) {
        return ApiResponse.success(botService.previewWorkflowRoute(id, body.message()));
    }

    private Map<String, Object> safeInput(Map<String, Object> input) {
        return input == null ? Map.of() : input;
    }

    public record SaveBotRequest(
            @NotBlank String name,
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
    }

    public record RunBotRequest(String message, Map<String, Object> input) {
    }

    public record ChatBotRequest(String sessionId, String message, Map<String, Object> input) {
    }

    public record BotRunResponse(AiBot bot, WorkflowExecutionResponse execution) {
    }

    public record BotChatResponse(
            BotSession session,
            List<BotMessage> messages,
            BotMessage reply,
            WorkflowExecutionResponse execution
    ) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record SaveBotCapabilityRequest(
            String capabilityType,
            String capabilityId,
            String capabilityCode,
            String routingKeywords,
            Boolean primaryCapability,
            Boolean enabled
    ) {
    }

    public record RoutePreviewRequest(String message) {
    }
}

