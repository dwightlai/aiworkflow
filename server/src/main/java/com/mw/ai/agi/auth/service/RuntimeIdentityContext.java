package com.mw.ai.agi.auth.service;

import java.util.List;

public record RuntimeIdentityContext(
        String tenantId,
        String appId,
        String userId,
        List<String> unitIds,
        String activeUnitId,
        List<String> departmentIds,
        List<String> roleIds,
        String authType,
        String source
) {
    public RuntimeIdentityContext {
        unitIds = unitIds == null ? List.of() : List.copyOf(unitIds);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
