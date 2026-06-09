package com.mw.ai.agi.auth.service;

public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private static volatile String defaultTenantId = "tenant_default";

    private TenantContext() {
    }

    public static void setDefaultTenantId(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            defaultTenantId = tenantId;
        }
    }

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String requireTenantId() {
        String tenantId = CURRENT.get();
        return tenantId == null || tenantId.isBlank() ? defaultTenantId : tenantId;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static String normalize(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return defaultTenantId;
        }
        if ("tenant-default".equals(tenantId) || "default".equals(tenantId)) {
            return defaultTenantId;
        }
        return tenantId;
    }
}
