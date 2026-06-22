package com.mw.ai.agi.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.chat.domain.AgentJob;
import com.mw.ai.agi.chat.persistence.AgentJobEntity;
import com.mw.ai.agi.chat.persistence.AgentJobMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentJobService {
    private final ObjectProvider<AgentJobMapper> mapperProvider;
    private final Map<String, AgentJobEntity> memoryJobs = new ConcurrentHashMap<>();

    public AgentJobService(ObjectProvider<AgentJobMapper> mapperProvider) {
        this.mapperProvider = mapperProvider;
    }

    public AgentJob create(
            String userId,
            String botId,
            String conversationId,
            String jobType,
            String sourceJobId
    ) {
        Instant now = Instant.now();
        AgentJobEntity entity = new AgentJobEntity();
        entity.setId("ajob_" + UUID.randomUUID());
        entity.setTenantId(TenantContext.requireTenantId());
        entity.setUserId(userId);
        entity.setBotId(botId);
        entity.setConversationId(conversationId);
        entity.setSourceJobId(sourceJobId);
        entity.setJobType(jobType == null || jobType.isBlank() ? "GENERIC" : jobType);
        entity.setStatus("PENDING");
        entity.setProgress(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        save(entity);
        return toDomain(entity);
    }

    public AgentJob get(String jobId) {
        AgentJobEntity entity = load(jobId);
        if (entity == null) {
            throw new IllegalArgumentException("Agent job not found: " + jobId);
        }
        return toDomain(entity);
    }

    public AgentJob getByAnyId(String jobId) {
        AgentJobEntity entity = loadAny(jobId);
        if (entity == null) {
            throw new IllegalArgumentException("Agent job not found: " + jobId);
        }
        return toDomain(entity);
    }

    public AgentJob getForUser(String jobId, String userId) {
        AgentJobEntity entity = loadAny(jobId);
        if (entity == null) {
            throw new IllegalArgumentException("Agent job not found: " + jobId);
        }
        if (!userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("Agent job not accessible");
        }
        return toDomain(entity);
    }

    public AgentJob upsertFromOutput(
            String userId,
            String botId,
            String conversationId,
            Map<String, Object> output
    ) {
        if (output == null || output.isEmpty()) {
            return null;
        }
        String explicitId = stringValue(output.get("agentJobId"));
        String sourceJobId = firstNonBlank(output.get("jobId"), output.get("sourceJobId"));
        if (explicitId.isBlank() && sourceJobId.isBlank()) {
            return null;
        }
        AgentJobEntity entity = !explicitId.isBlank() ? load(explicitId) : findBySourceJobId(sourceJobId);
        Instant now = Instant.now();
        if (entity == null) {
            entity = new AgentJobEntity();
            entity.setId(!explicitId.isBlank() ? explicitId : "ajob_" + UUID.randomUUID());
            entity.setTenantId(TenantContext.requireTenantId());
            entity.setUserId(userId);
            entity.setBotId(botId);
            entity.setConversationId(conversationId);
            entity.setSourceJobId(sourceJobId.isBlank() ? null : sourceJobId);
            entity.setJobType(stringValue(output.get("jobType")).isBlank() ? "GENERIC" : stringValue(output.get("jobType")));
            entity.setCreatedAt(now);
        }
        String status = stringValue(output.get("jobStatus"));
        if (!status.isBlank()) {
            entity.setStatus(status);
        } else if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("RUNNING");
        }
        Object progress = output.get("progress");
        if (progress instanceof Number number) {
            entity.setProgress(Math.max(0, Math.min(100, number.intValue())));
        }
        String currentStep = stringValue(output.get("currentStep"));
        if (!currentStep.isBlank()) {
            entity.setCurrentStep(currentStep);
        }
        String errorMessage = stringValue(output.get("errorMessage"));
        if (!errorMessage.isBlank()) {
            entity.setErrorMessage(errorMessage);
        }
        if ("COMPLETED".equalsIgnoreCase(entity.getStatus()) || "SUCCEEDED".equalsIgnoreCase(entity.getStatus())) {
            entity.setProgress(100);
            String result = buildResultJson(output);
            if (!result.isBlank()) {
                entity.setResult(result);
            }
        }
        entity.setUpdatedAt(now);
        save(entity);
        return toDomain(entity);
    }

    public AgentJob applyOpenUpdate(String jobId, Map<String, Object> patch) {
        AgentJobEntity entity = requireAny(jobId);
        if (patch == null || patch.isEmpty()) {
            return toDomain(entity);
        }
        String status = stringValue(patch.get("status"));
        if (!status.isBlank()) {
            entity.setStatus(status);
        }
        Object progress = patch.get("progress");
        if (progress instanceof Number number) {
            entity.setProgress(Math.max(0, Math.min(100, number.intValue())));
        }
        String currentStep = stringValue(patch.get("currentStep"));
        if (!currentStep.isBlank()) {
            entity.setCurrentStep(currentStep);
        }
        String errorMessage = stringValue(patch.get("errorMessage"));
        if (!errorMessage.isBlank()) {
            entity.setErrorMessage(errorMessage);
        }
        Object result = patch.get("result");
        if (result != null) {
            entity.setResult(result instanceof String text ? text : String.valueOf(result));
        }
        if ("COMPLETED".equalsIgnoreCase(entity.getStatus()) || "SUCCEEDED".equalsIgnoreCase(entity.getStatus())) {
            entity.setProgress(100);
        }
        entity.setUpdatedAt(Instant.now());
        save(entity);
        return toDomain(entity);
    }

    public List<AgentJob> list(String botId, String status, String conversationId, int limit) {
        AgentJobMapper mapper = mapperProvider.getIfAvailable();
        int effectiveLimit = limit <= 0 ? 50 : Math.min(limit, 200);
        if (mapper == null) {
            return memoryJobs.values().stream()
                    .filter(item -> botId == null || botId.isBlank() || botId.equals(item.getBotId()))
                    .filter(item -> status == null || status.isBlank() || status.equalsIgnoreCase(item.getStatus()))
                    .filter(item -> conversationId == null || conversationId.isBlank() || conversationId.equals(item.getConversationId()))
                    .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                    .limit(effectiveLimit)
                    .map(this::toDomain)
                    .toList();
        }
        LambdaQueryWrapper<AgentJobEntity> wrapper = new LambdaQueryWrapper<AgentJobEntity>()
                .eq(AgentJobEntity::getTenantId, TenantContext.requireTenantId())
                .orderByDesc(AgentJobEntity::getUpdatedAt)
                .last("LIMIT " + effectiveLimit);
        if (botId != null && !botId.isBlank()) {
            wrapper.eq(AgentJobEntity::getBotId, botId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AgentJobEntity::getStatus, status);
        }
        if (conversationId != null && !conversationId.isBlank()) {
            wrapper.eq(AgentJobEntity::getConversationId, conversationId);
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    public List<AgentJob> listByConversation(String conversationId, int limit) {
        AgentJobMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            int effectiveLimit = limit <= 0 ? 20 : Math.min(limit, 100);
            return memoryJobs.values().stream()
                    .filter(item -> conversationId.equals(item.getConversationId()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(effectiveLimit)
                    .map(this::toDomain)
                    .toList();
        }
        int effectiveLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        LambdaQueryWrapper<AgentJobEntity> wrapper = new LambdaQueryWrapper<AgentJobEntity>()
                .eq(AgentJobEntity::getTenantId, TenantContext.requireTenantId())
                .eq(AgentJobEntity::getConversationId, conversationId)
                .orderByDesc(AgentJobEntity::getCreatedAt)
                .last("LIMIT " + effectiveLimit);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    public AgentJob updateProgress(String jobId, int progress, String currentStep, String status) {
        AgentJobEntity entity = require(jobId);
        entity.setProgress(Math.max(0, Math.min(100, progress)));
        if (currentStep != null) {
            entity.setCurrentStep(currentStep);
        }
        if (status != null && !status.isBlank()) {
            entity.setStatus(status);
        }
        entity.setUpdatedAt(Instant.now());
        save(entity);
        return toDomain(entity);
    }

    public AgentJob complete(String jobId, String result) {
        AgentJobEntity entity = require(jobId);
        entity.setStatus("COMPLETED");
        entity.setProgress(100);
        entity.setResult(result);
        entity.setUpdatedAt(Instant.now());
        save(entity);
        return toDomain(entity);
    }

    public AgentJob fail(String jobId, String errorMessage) {
        AgentJobEntity entity = require(jobId);
        entity.setStatus("FAILED");
        entity.setErrorMessage(errorMessage);
        entity.setUpdatedAt(Instant.now());
        save(entity);
        return toDomain(entity);
    }

    private AgentJobEntity requireAny(String jobId) {
        AgentJobEntity entity = loadAny(jobId);
        if (entity == null) {
            throw new IllegalArgumentException("Agent job not found: " + jobId);
        }
        return entity;
    }

    private AgentJobEntity loadAny(String jobId) {
        AgentJobEntity entity = load(jobId);
        if (entity != null) {
            return entity;
        }
        return findBySourceJobId(jobId);
    }

    private AgentJobEntity findBySourceJobId(String sourceJobId) {
        if (sourceJobId == null || sourceJobId.isBlank()) {
            return null;
        }
        AgentJobMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            return memoryJobs.values().stream()
                    .filter(item -> sourceJobId.equals(item.getSourceJobId()))
                    .findFirst()
                    .orElse(null);
        }
        return mapper.selectOne(new LambdaQueryWrapper<AgentJobEntity>()
                .eq(AgentJobEntity::getTenantId, TenantContext.requireTenantId())
                .eq(AgentJobEntity::getSourceJobId, sourceJobId)
                .last("LIMIT 1"));
    }

    private String buildResultJson(Map<String, Object> output) {
        String result = stringValue(output.get("result"));
        if (!result.isBlank()) {
            return result;
        }
        String downloadUrl = stringValue(output.get("downloadUrl"));
        String resultUrl = stringValue(output.get("resultUrl"));
        String title = stringValue(output.get("title"));
        if (downloadUrl.isBlank() && resultUrl.isBlank() && title.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        if (!title.isBlank()) {
            builder.append("\"title\":\"").append(escapeJson(title)).append("\"");
            first = false;
        }
        if (!downloadUrl.isBlank()) {
            if (!first) {
                builder.append(',');
            }
            builder.append("\"downloadUrl\":\"").append(escapeJson(downloadUrl)).append("\"");
            first = false;
        }
        if (!resultUrl.isBlank()) {
            if (!first) {
                builder.append(',');
            }
            builder.append("\"resultUrl\":\"").append(escapeJson(resultUrl)).append("\"");
        }
        builder.append('}');
        return builder.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String firstNonBlank(Object left, Object right) {
        String leftText = stringValue(left);
        if (!leftText.isBlank()) {
            return leftText;
        }
        return stringValue(right);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private AgentJobEntity require(String jobId) {
        AgentJobEntity entity = load(jobId);
        if (entity == null) {
            throw new IllegalArgumentException("Agent job not found: " + jobId);
        }
        return entity;
    }

    private AgentJobEntity load(String jobId) {
        AgentJobMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            return memoryJobs.get(jobId);
        }
        return mapper.selectById(jobId);
    }

    private void save(AgentJobEntity entity) {
        AgentJobMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            memoryJobs.put(entity.getId(), entity);
            return;
        }
        if (mapper.selectById(entity.getId()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
    }

    private AgentJob toDomain(AgentJobEntity entity) {
        return new AgentJob(
                entity.getId(),
                entity.getBotId(),
                entity.getConversationId(),
                entity.getSourceJobId(),
                entity.getJobType(),
                entity.getStatus(),
                entity.getProgress(),
                entity.getCurrentStep(),
                entity.getResult(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
