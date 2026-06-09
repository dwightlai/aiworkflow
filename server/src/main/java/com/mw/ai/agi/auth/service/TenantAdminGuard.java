package com.mw.ai.agi.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TenantAdminGuard {
    private final String defaultTenantId;

    public TenantAdminGuard(@Value("${agi.auth.default-tenant-id:tenant_default}") String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public String currentTenantId() {
        return TenantContext.requireTenantId();
    }

    public boolean isPlatformOperator() {
        return AuthRequestContext.current()
                .map(holder -> defaultTenantId.equals(TenantContext.normalize(holder.tenantId()))
                        && holder.roleIds().contains("platform_admin"))
                .orElse(false);
    }

    public void assertPlatformOperator() {
        if (AuthRequestContext.current().isEmpty()) {
            return;
        }
        if (!isPlatformOperator()) {
            throw new AuthException("PLATFORM_OPERATOR_REQUIRED", HttpStatus.FORBIDDEN, "Platform operator is required.");
        }
    }

    public void assertCanAccessTenant(String targetTenantId) {
        if (AuthRequestContext.current().isEmpty()) {
            return;
        }
        String normalizedTarget = TenantContext.normalize(targetTenantId);
        if (normalizedTarget.equals(currentTenantId())) {
            return;
        }
        if (isPlatformOperator()) {
            return;
        }
        throw new AuthException("TENANT_ACCESS_DENIED", HttpStatus.FORBIDDEN, "Tenant access denied.");
    }
}
