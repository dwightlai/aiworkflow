package com.mw.ai.agi.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.persistence.AuthAuditLogEntity;
import com.mw.ai.agi.auth.persistence.AuthAuditLogMapper;
import com.mw.ai.agi.auth.service.TenantAdminGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuthAuditLogQueryService {
    private final AuthAuditLogMapper auditLogMapper;
    private final TenantAdminGuard tenantAdminGuard;

    public AuthAuditLogQueryService(AuthAuditLogMapper auditLogMapper, TenantAdminGuard tenantAdminGuard) {
        this.auditLogMapper = auditLogMapper;
        this.tenantAdminGuard = tenantAdminGuard;
    }

    public PageResult<AuthAuditLogEntity> search(
            String eventType,
            String userId,
            String result,
            Instant from,
            Instant to,
            int page,
            int pageSize
    ) {
        tenantAdminGuard.assertAuditViewer();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 200);
        LambdaQueryWrapper<AuthAuditLogEntity> filter = buildFilter(eventType, userId, result, from, to);
        long total = auditLogMapper.selectCount(filter);
        int offset = (safePage - 1) * safeSize;
        List<AuthAuditLogEntity> items = auditLogMapper.selectList(buildFilter(eventType, userId, result, from, to)
                .orderByDesc(AuthAuditLogEntity::getOccurredAt)
                .select(
                        AuthAuditLogEntity::getId,
                        AuthAuditLogEntity::getTenantId,
                        AuthAuditLogEntity::getEventType,
                        AuthAuditLogEntity::getUserId,
                        AuthAuditLogEntity::getAppId,
                        AuthAuditLogEntity::getUnitId,
                        AuthAuditLogEntity::getClientIp,
                        AuthAuditLogEntity::getResult,
                        AuthAuditLogEntity::getErrorCode,
                        AuthAuditLogEntity::getOccurredAt
                )
                .last("LIMIT " + safeSize + " OFFSET " + offset));
        return new PageResult<>(items, total);
    }

    public List<String> listEventTypes() {
        tenantAdminGuard.assertAuditViewer();
        return auditLogMapper.selectDistinctEventTypes(tenantId());
    }

    private LambdaQueryWrapper<AuthAuditLogEntity> buildFilter(
            String eventType,
            String userId,
            String result,
            Instant from,
            Instant to
    ) {
        LambdaQueryWrapper<AuthAuditLogEntity> wrapper = new LambdaQueryWrapper<AuthAuditLogEntity>()
                .eq(AuthAuditLogEntity::getTenantId, tenantId());
        if (eventType != null && !eventType.isBlank()) {
            wrapper.eq(AuthAuditLogEntity::getEventType, eventType.trim());
        }
        if (userId != null && !userId.isBlank()) {
            wrapper.eq(AuthAuditLogEntity::getUserId, userId.trim());
        }
        if (result != null && !result.isBlank()) {
            wrapper.eq(AuthAuditLogEntity::getResult, result.trim());
        }
        if (from != null) {
            wrapper.ge(AuthAuditLogEntity::getOccurredAt, from);
        }
        if (to != null) {
            wrapper.le(AuthAuditLogEntity::getOccurredAt, to);
        }
        return wrapper;
    }

    private String tenantId() {
        return TenantContext.requireTenantId();
    }

    public record PageResult<T>(List<T> items, long total) {
    }
}
