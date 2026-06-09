package com.mw.ai.agi.auth.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TenantBusinessGuard {
    private final String configuredDefaultTenantId;

    public TenantBusinessGuard(@Value("${agi.auth.default-tenant-id:tenant_default}") String configuredDefaultTenantId) {
        this.configuredDefaultTenantId = configuredDefaultTenantId;
    }

    @PostConstruct
    void init() {
        TenantContext.setDefaultTenantId(configuredDefaultTenantId);
    }

    public String currentTenantId() {
        return TenantContext.requireTenantId();
    }

    public void assertAccessible(String resourceTenantId) {
        if (!matches(resourceTenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }

    public boolean matches(String resourceTenantId) {
        return normalize(resourceTenantId).equals(currentTenantId());
    }

    public String normalize(String tenantId) {
        return TenantContext.normalize(tenantId);
    }
}
