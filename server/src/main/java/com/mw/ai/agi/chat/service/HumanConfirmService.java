package com.mw.ai.agi.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.chat.persistence.HumanConfirmTaskEntity;
import com.mw.ai.agi.chat.persistence.HumanConfirmTaskMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HumanConfirmService {
    private static final long DEFAULT_EXPIRE_MINUTES = 30;

    private final ObjectProvider<HumanConfirmTaskMapper> mapperProvider;
    private final ObjectMapper objectMapper;
    private final Map<String, HumanConfirmTaskEntity> memoryTasks = new ConcurrentHashMap<>();

    public HumanConfirmService(ObjectProvider<HumanConfirmTaskMapper> mapperProvider, ObjectMapper objectMapper) {
        this.mapperProvider = mapperProvider;
        this.objectMapper = objectMapper;
    }

    public String createTask(
            String conversationId,
            String botId,
            String workflowRunId,
            String nodeId,
            String userId,
            String title,
            String summary,
            Map<String, Object> payloadSnapshot,
            String connectorCode,
            String operationCode,
            String payloadHash
    ) {
        Instant now = Instant.now();
        HumanConfirmTaskEntity entity = new HumanConfirmTaskEntity();
        entity.setId("hct_" + UUID.randomUUID());
        entity.setTenantId(TenantContext.requireTenantId());
        entity.setConversationId(conversationId);
        entity.setBotId(botId);
        entity.setWorkflowRunId(workflowRunId);
        entity.setNodeId(nodeId);
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setPayloadSnapshot(writeJson(payloadSnapshot));
        entity.setConnectorCode(connectorCode);
        entity.setOperationCode(operationCode);
        entity.setPayloadHash(payloadHash);
        entity.setStatus("PENDING");
        entity.setExpireAt(now.plusSeconds(DEFAULT_EXPIRE_MINUTES * 60));
        entity.setCreatedAt(now);
        HumanConfirmTaskMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            mapper.insert(entity);
        } else {
            memoryTasks.put(entity.getId(), entity);
        }
        return entity.getId();
    }

    public HumanConfirmTaskEntity getTask(String taskId) {
        HumanConfirmTaskMapper mapper = mapperProvider.getIfAvailable();
        HumanConfirmTaskEntity entity = mapper == null ? memoryTasks.get(taskId) : mapper.selectById(taskId);
        if (entity == null) {
            throw new IllegalArgumentException("Confirm task not found: " + taskId);
        }
        return entity;
    }

    public boolean isConfirmed(String taskId, String payloadHash) {
        HumanConfirmTaskEntity entity = getTask(taskId);
        return "CONFIRMED".equals(entity.getStatus()) && payloadHash.equals(entity.getPayloadHash());
    }

    public HumanConfirmTaskEntity confirm(String taskId, String userId) {
        HumanConfirmTaskEntity entity = getTask(taskId);
        assertPending(entity, userId);
        entity.setStatus("CONFIRMED");
        entity.setConfirmedAt(Instant.now());
        save(entity);
        return entity;
    }

    public HumanConfirmTaskEntity reject(String taskId, String userId) {
        HumanConfirmTaskEntity entity = getTask(taskId);
        assertPending(entity, userId);
        entity.setStatus("REJECTED");
        entity.setConfirmedAt(Instant.now());
        save(entity);
        return entity;
    }

    private void assertPending(HumanConfirmTaskEntity entity, String userId) {
        if (!entity.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Confirm task user mismatch");
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new IllegalStateException("Confirm task is not pending");
        }
        if (entity.getExpireAt() != null && Instant.now().isAfter(entity.getExpireAt())) {
            entity.setStatus("EXPIRED");
            save(entity);
            throw new IllegalStateException("Confirm task expired");
        }
    }

    private void save(HumanConfirmTaskEntity entity) {
        HumanConfirmTaskMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            mapper.updateById(entity);
        } else {
            memoryTasks.put(entity.getId(), entity);
        }
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
