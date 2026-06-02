package com.aiworkflow.bot.api;

import com.aiworkflow.bot.domain.AiBot;
import com.aiworkflow.bot.domain.BotChatResult;
import com.aiworkflow.bot.domain.BotMessage;
import com.aiworkflow.bot.domain.BotRunResult;
import com.aiworkflow.bot.domain.BotSession;
import com.aiworkflow.bot.domain.BotStatus;
import com.aiworkflow.bot.service.BotService;
import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.workflow.api.WorkflowExecutionResponse;
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

    public BotController(BotService botService) {
        this.botService = botService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiBot>> list() {
        List<AiBot> bots = botService.list();
        return ApiResponse.success(new PageResponse<>(bots, bots.size()));
    }

    @PostMapping
    public ApiResponse<AiBot> create(@Valid @RequestBody SaveBotRequest request) {
        return ApiResponse.success(botService.create(
                request.name(),
                request.description(),
                request.avatar(),
                request.workflowId(),
                request.modelProviderId(),
                request.knowledgeBaseId(),
                request.systemPrompt(),
                request.openingMessage(),
                request.status()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiBot> update(@PathVariable String id, @Valid @RequestBody SaveBotRequest request) {
        return ApiResponse.success(botService.update(
                id,
                request.name(),
                request.description(),
                request.avatar(),
                request.workflowId(),
                request.modelProviderId(),
                request.knowledgeBaseId(),
                request.systemPrompt(),
                request.openingMessage(),
                request.status()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        botService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/run")
    public ApiResponse<BotRunResponse> run(@PathVariable String id, @RequestBody(required = false) RunBotRequest request) {
        RunBotRequest safeRequest = request == null ? new RunBotRequest("", Map.of()) : request;
        BotRunResult result = botService.run(id, safeRequest.message(), safeRequest.input());
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
    public ApiResponse<BotChatResponse> chat(@PathVariable String id, @RequestBody(required = false) ChatBotRequest request) {
        ChatBotRequest safeRequest = request == null ? new ChatBotRequest(null, "", Map.of()) : request;
        BotChatResult result = botService.chat(id, safeRequest.sessionId(), safeRequest.message(), safeRequest.input());
        return ApiResponse.success(new BotChatResponse(
                result.session(),
                result.messages(),
                result.reply(),
                WorkflowExecutionResponse.from(result.execution())
        ));
    }

    public record SaveBotRequest(
            @NotBlank String name,
            String description,
            String avatar,
            @NotBlank String workflowId,
            String modelProviderId,
            String knowledgeBaseId,
            String systemPrompt,
            String openingMessage,
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
}
