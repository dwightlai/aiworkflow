package com.mw.ai.agi.openapi.client.feign;

import com.mw.ai.agi.openapi.client.model.ApiResponse;
import com.mw.ai.agi.openapi.client.model.PageResponse;
import com.mw.ai.agi.openapi.client.model.bot.OpenBotCapabilityView;
import com.mw.ai.agi.openapi.client.model.bot.OpenBotChatRequest;
import com.mw.ai.agi.openapi.client.model.bot.OpenBotChatResponse;
import com.mw.ai.agi.openapi.client.model.bot.OpenBotCreateSessionRequest;
import com.mw.ai.agi.openapi.client.model.bot.OpenBotRunRequest;
import com.mw.ai.agi.openapi.client.model.bot.OpenBotRunResponse;
import com.mw.ai.agi.openapi.client.model.bot.OpenBotView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        contextId = "agiOpenBotClient",
        name = "${agi.openapi.service-name:aiworkflow-server}",
        url = "${agi.openapi.base-url:}"
)
public interface AgiOpenBotClient {

    @GetMapping("/api/open/bots")
    ApiResponse<PageResponse<OpenBotView>> listBots();

    @GetMapping("/api/open/bots/{id}")
    ApiResponse<OpenBotView> getBot(@PathVariable("id") String id);

    @GetMapping("/api/open/bots/{id}/capabilities")
    ApiResponse<PageResponse<OpenBotCapabilityView>> listBotCapabilities(@PathVariable("id") String id);

    @GetMapping("/api/open/bots/{id}/sessions")
    ApiResponse<PageResponse<Object>> listBotSessions(@PathVariable("id") String id);

    @PostMapping("/api/open/bots/{id}/sessions")
    ApiResponse<Object> createBotSession(
            @PathVariable("id") String id,
            @RequestBody(required = false) OpenBotCreateSessionRequest request
    );

    @GetMapping("/api/open/bots/{id}/sessions/{sessionId}/messages")
    ApiResponse<PageResponse<Object>> listBotMessages(
            @PathVariable("id") String id,
            @PathVariable("sessionId") String sessionId
    );

    @PostMapping("/api/open/bots/{id}/run")
    ApiResponse<OpenBotRunResponse> runBot(@PathVariable("id") String id, @RequestBody(required = false) OpenBotRunRequest request);

    @PostMapping("/api/open/bots/{id}/chat")
    ApiResponse<OpenBotChatResponse> chatBot(@PathVariable("id") String id, @RequestBody(required = false) OpenBotChatRequest request);
}
