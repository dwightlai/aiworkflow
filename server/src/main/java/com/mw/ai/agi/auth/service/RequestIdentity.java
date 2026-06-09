package com.mw.ai.agi.auth.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RequestIdentity(
        String userId,
        String tenantId,
        String activeUnitId,
        List<String> unitIds,
        List<String> departmentIds
) {
    public Map<String, Object> toGrantContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("userId", userId);
        context.put("tenantId", tenantId);
        context.put("activeUnitId", activeUnitId);
        context.put("unitIds", unitIds);
        context.put("departmentIds", departmentIds);
        return context;
    }
}
