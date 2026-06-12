package com.mw.ai.agi.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.chat.persistence.AgentAuditLogEntity;
import com.mw.ai.agi.chat.persistence.AgentAuditLogMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentAuditService {
    private final ObjectProvider<AgentAuditLogMapper> mapperProvider;

    public AgentAuditService(ObjectProvider<AgentAuditLogMapper> mapperProvider) {
        this.mapperProvider = mapperProvider;
    }

    public void log(
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
            String traceId
    ) {
        AgentAuditLogMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            return;
        }
        AgentAuditLogEntity entity = new AgentAuditLogEntity();
        entity.setId("audit_" + UUID.randomUUID());
        entity.setTenantId(TenantContext.requireTenantId());
        entity.setUserId(userId);
        entity.setBotId(botId);
        entity.setConversationId(conversationId);
        entity.setMessageId(messageId);
        entity.setConnectorCode(connectorCode);
        entity.setOperationCode(operationCode);
        entity.setEventType(eventType);
        entity.setRequestSummary(requestSummary);
        entity.setResponseSummary(responseSummary);
        entity.setStatus(status);
        entity.setErrorMessage(errorMessage);
        entity.setTraceId(traceId);
        entity.setCreatedAt(Instant.now());
        mapper.insert(entity);
    }

    public List<AgentAuditLogEntity> list(String traceId, String botId, String eventType, int limit) {
        AgentAuditLogMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            return List.of();
        }
        int effectiveLimit = limit <= 0 ? 50 : Math.min(limit, 200);
        LambdaQueryWrapper<AgentAuditLogEntity> wrapper = new LambdaQueryWrapper<AgentAuditLogEntity>()
                .eq(AgentAuditLogEntity::getTenantId, TenantContext.requireTenantId())
                .orderByDesc(AgentAuditLogEntity::getCreatedAt)
                .last("LIMIT " + effectiveLimit);
        if (traceId != null && !traceId.isBlank()) {
            wrapper.eq(AgentAuditLogEntity::getTraceId, traceId);
        }
        if (botId != null && !botId.isBlank()) {
            wrapper.eq(AgentAuditLogEntity::getBotId, botId);
        }
        if (eventType != null && !eventType.isBlank()) {
            wrapper.eq(AgentAuditLogEntity::getEventType, eventType);
        }
        return mapper.selectList(wrapper);
    }
}
