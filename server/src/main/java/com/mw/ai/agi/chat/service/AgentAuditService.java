package com.mw.ai.agi.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.chat.persistence.AgentAuditLogEntity;
import com.mw.ai.agi.chat.persistence.AgentAuditLogMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        entity.setTenantId(fit(TenantContext.requireTenantId(), 64));
        entity.setUserId(fit(userId, 64));
        entity.setBotId(fit(botId, 64));
        entity.setConversationId(fit(conversationId, 64));
        entity.setMessageId(fit(messageId, 64));
        entity.setConnectorCode(fit(connectorCode, 64));
        entity.setOperationCode(fit(operationCode, 64));
        entity.setEventType(fit(eventType, 64));
        entity.setRequestSummary(requestSummary);
        entity.setResponseSummary(responseSummary);
        entity.setStatus(fit(status, 32));
        entity.setErrorMessage(errorMessage);
        entity.setTraceId(fit(traceId, 64));
        entity.setCreatedAt(Instant.now());
        try {
            mapper.insert(entity);
        } catch (RuntimeException ignored) {
        }
    }

    public List<ConnectorCallStat> connectorCallStats(int limit) {
        AgentAuditLogMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            return List.of();
        }
        int effectiveLimit = limit <= 0 ? 2000 : Math.min(limit, 5000);
        List<AgentAuditLogEntity> logs = mapper.selectList(new LambdaQueryWrapper<AgentAuditLogEntity>()
                .eq(AgentAuditLogEntity::getTenantId, TenantContext.requireTenantId())
                .eq(AgentAuditLogEntity::getEventType, "CONNECTOR_CALL")
                .orderByDesc(AgentAuditLogEntity::getCreatedAt)
                .last("LIMIT " + effectiveLimit));
        Map<String, ConnectorCallStat> grouped = new LinkedHashMap<>();
        for (AgentAuditLogEntity item : logs) {
            String connectorCode = item.getConnectorCode() == null ? "" : item.getConnectorCode();
            String operationCode = item.getOperationCode() == null ? "" : item.getOperationCode();
            String key = connectorCode + "::" + operationCode;
            ConnectorCallStat current = grouped.computeIfAbsent(key, ignored -> new ConnectorCallStat(
                    connectorCode,
                    operationCode,
                    0,
                    0,
                    0
            ));
            int total = current.totalCalls() + 1;
            int success = current.successCalls();
            int failed = current.failedCalls();
            if ("SUCCESS".equalsIgnoreCase(item.getStatus())) {
                success += 1;
            } else {
                failed += 1;
            }
            grouped.put(key, new ConnectorCallStat(connectorCode, operationCode, total, success, failed));
        }
        return grouped.values().stream()
                .sorted(Comparator.comparingInt(ConnectorCallStat::totalCalls).reversed())
                .toList();
    }

    public List<AgentAuditLogEntity> list(
            String traceId,
            String botId,
            String eventType,
            String connectorCode,
            String operationCode,
            String status,
            int limit
    ) {
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
        if (connectorCode != null && !connectorCode.isBlank()) {
            wrapper.eq(AgentAuditLogEntity::getConnectorCode, connectorCode);
        }
        if (operationCode != null && !operationCode.isBlank()) {
            wrapper.eq(AgentAuditLogEntity::getOperationCode, operationCode);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AgentAuditLogEntity::getStatus, status);
        }
        return mapper.selectList(wrapper);
    }

    private static String fit(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    public record ConnectorCallStat(
            String connectorCode,
            String operationCode,
            int totalCalls,
            int successCalls,
            int failedCalls
    ) {
    }
}
