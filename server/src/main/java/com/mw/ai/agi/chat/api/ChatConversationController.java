package com.mw.ai.agi.chat.api;

import com.mw.ai.agi.bot.domain.BotMessage;
import com.mw.ai.agi.bot.domain.BotSession;
import com.mw.ai.agi.chat.service.ChatGatewayService;
import com.mw.ai.agi.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/conversations")
public class ChatConversationController {
    private final ChatGatewayService chatGatewayService;

    public ChatConversationController(ChatGatewayService chatGatewayService) {
        this.chatGatewayService = chatGatewayService;
    }

    @GetMapping
    public ApiResponse<ChatController.PageResponse<BotSession>> list(
            @RequestParam String botId,
            HttpServletRequest request
    ) {
        List<BotSession> sessions = chatGatewayService.listSessions(botId, request);
        return ApiResponse.success(new ChatController.PageResponse<>(sessions, sessions.size()));
    }

    @PostMapping
    public ApiResponse<BotSession> create(
            @RequestParam String botId,
            @RequestBody(required = false) ChatController.CreateSessionRequest body,
            HttpServletRequest request
    ) {
        ChatController.CreateSessionRequest safe = body == null ? new ChatController.CreateSessionRequest("新对话") : body;
        return ApiResponse.success(chatGatewayService.createSession(botId, safe.title(), request));
    }

    @DeleteMapping("/{conversationId}")
    public ApiResponse<Map<String, Boolean>> delete(
            @RequestParam String botId,
            @PathVariable String conversationId,
            HttpServletRequest request
    ) {
        chatGatewayService.deleteSession(botId, conversationId, request);
        return ApiResponse.success(Map.of("deleted", true));
    }

    @PatchMapping("/{conversationId}")
    public ApiResponse<BotSession> update(
            @RequestParam String botId,
            @PathVariable String conversationId,
            @RequestBody(required = false) ChatController.UpdateSessionRequest body,
            HttpServletRequest request
    ) {
        ChatController.UpdateSessionRequest safe = body == null ? new ChatController.UpdateSessionRequest(null, null) : body;
        return ApiResponse.success(chatGatewayService.updateSession(
                botId,
                conversationId,
                safe.title(),
                safe.pinned(),
                request
        ));
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<ChatController.PageResponse<BotMessage>> messages(
            @RequestParam String botId,
            @PathVariable String conversationId,
            HttpServletRequest request
    ) {
        List<BotMessage> messages = chatGatewayService.listMessages(botId, conversationId, request);
        return ApiResponse.success(new ChatController.PageResponse<>(messages, messages.size()));
    }

    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @RequestParam String botId,
            @PathVariable String conversationId,
            @RequestBody(required = false) ChatController.StreamMessageRequest body,
            HttpServletRequest request
    ) {
        ChatController.StreamMessageRequest safe = body == null ? new ChatController.StreamMessageRequest("") : body;
        return chatGatewayService.streamChat(botId, conversationId, safe.message(), request);
    }
}
