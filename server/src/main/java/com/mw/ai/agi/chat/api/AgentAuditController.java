package com.mw.ai.agi.chat.api;

import com.mw.ai.agi.chat.persistence.AgentAuditLogEntity;
import com.mw.ai.agi.chat.service.AgentAuditService;
import com.mw.ai.agi.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/agent-audit")
public class AgentAuditController {
    private final AgentAuditService agentAuditService;

    public AgentAuditController(AgentAuditService agentAuditService) {
        this.agentAuditService = agentAuditService;
    }

    @GetMapping("/logs")
    public ApiResponse<PageResponse<AgentAuditLogView>> list(
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String botId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String connectorCode,
            @RequestParam(required = false) String operationCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<AgentAuditLogView> items = agentAuditService.list(
                traceId,
                botId,
                eventType,
                connectorCode,
                operationCode,
                status,
                limit
        ).stream()
                .map(AgentAuditLogView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record AgentAuditLogView(
            String id,
            String userId,
            String botId,
            String conversationId,
            String messageId,
            String connectorCode,
            String operationCode,
            String eventType,
            String requestSummary,
            String responseSummary,
            String status,
            String errorMessage,
            String traceId,
            Instant createdAt
    ) {
        static AgentAuditLogView from(AgentAuditLogEntity entity) {
            return new AgentAuditLogView(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getBotId(),
                    entity.getConversationId(),
                    entity.getMessageId(),
                    entity.getConnectorCode(),
                    entity.getOperationCode(),
                    entity.getEventType(),
                    entity.getRequestSummary(),
                    entity.getResponseSummary(),
                    entity.getStatus(),
                    entity.getErrorMessage(),
                    entity.getTraceId(),
                    entity.getCreatedAt()
            );
        }
    }
}
