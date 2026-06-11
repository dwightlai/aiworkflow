package com.mw.ai.agi.system.service;

import com.mw.ai.agi.auth.persistence.AuthAuditLogEntity;
import com.mw.ai.agi.auth.persistence.AuthAuditLogMapper;
import com.mw.ai.agi.auth.service.AuthRequestContext;
import com.mw.ai.agi.auth.service.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SystemAuditService {
    private final AuthAuditLogMapper auditLogMapper;
    private final String defaultTenantId;

    public SystemAuditService(
            AuthAuditLogMapper auditLogMapper,
            @Value("${agi.auth.default-tenant-id:tenant_default}") String defaultTenantId
    ) {
        this.auditLogMapper = auditLogMapper;
        this.defaultTenantId = defaultTenantId;
    }

    public void recordSuccess(String eventType, String resourceId) {
        write(eventType, resourceId, "SUCCESS", null);
    }

    public void write(String eventType, String resourceId, String result, String errorCode) {
        AuthAuditLogEntity auditLog = new AuthAuditLogEntity();
        auditLog.setId("audit_" + UUID.randomUUID().toString().replace("-", ""));
        auditLog.setTenantId(TenantContext.requireTenantId());
        auditLog.setEventType(eventType);
        auditLog.setAppId(resourceId);
        auditLog.setResult(result);
        auditLog.setErrorCode(errorCode);
        auditLog.setOccurredAt(Instant.now());
        AuthRequestContext.current().ifPresent(holder -> {
            auditLog.setUserId(holder.userId());
            auditLog.setRoleIds(String.join(",", holder.roleIds()));
        });
        if (auditLog.getTenantId() == null || auditLog.getTenantId().isBlank()) {
            auditLog.setTenantId(defaultTenantId);
        }
        auditLogMapper.insert(auditLog);
    }
}
