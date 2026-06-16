package com.mw.ai.agi.auth.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class OpenApiRequestContext {
    public static final String ATTRIBUTE = "com.mw.ai.agi.openApiIdentity";

    private OpenApiRequestContext() {
    }

    public static void set(HttpServletRequest request, RuntimeIdentityContext context) {
        if (request != null && context != null) {
            request.setAttribute(ATTRIBUTE, context);
        }
    }

    public static Optional<RuntimeIdentityContext> get(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Object value = request.getAttribute(ATTRIBUTE);
        if (value instanceof RuntimeIdentityContext context) {
            return Optional.of(context);
        }
        return Optional.empty();
    }

    public static RuntimeIdentityContext require(HttpServletRequest request) {
        return get(request).orElseThrow(() -> new AuthException(
                "OPEN_API_AUTH_REQUIRED",
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Open API authentication is required."
        ));
    }

    public static Map<String, Object> auditContext(RuntimeIdentityContext context) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (context == null) {
            return map;
        }
        if (context.userId() != null && !context.userId().isBlank()) {
            map.put("userId", context.userId());
        }
        map.put("openApiAppId", context.appId());
        map.put("openApiAppCode", context.source());
        map.put("authType", context.authType());
        return map;
    }

    public static Map<String, Object> grantContext(RuntimeIdentityContext context) {
        Map<String, Object> map = new LinkedHashMap<>(auditContext(context));
        if (context == null) {
            return map;
        }
        if (context.tenantId() != null && !context.tenantId().isBlank()) {
            map.put("tenantId", context.tenantId());
        }
        if (context.activeUnitId() != null && !context.activeUnitId().isBlank()) {
            map.put("activeUnitId", context.activeUnitId());
        }
        if (!context.unitIds().isEmpty()) {
            map.put("unitIds", context.unitIds());
        }
        if (!context.departmentIds().isEmpty()) {
            map.put("departmentIds", context.departmentIds());
        }
        if (!context.roleIds().isEmpty()) {
            map.put("roleIds", context.roleIds());
        }
        return map;
    }
}
