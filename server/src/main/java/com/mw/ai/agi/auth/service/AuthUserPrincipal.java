package com.mw.ai.agi.auth.service;

import java.util.List;

public record AuthUserPrincipal(
        String id,
        String username,
        String tenantId,
        String displayName,
        String userType,
        Integer sortOrder,
        List<String> organizationIds,
        String activeOrganizationId,
        List<String> unitIds,
        String activeUnitId,
        List<String> departmentIds,
        List<String> roleIds
) {
    public AuthUserPrincipal {
        organizationIds = organizationIds == null ? List.of() : List.copyOf(organizationIds);
        unitIds = unitIds == null ? List.of() : List.copyOf(unitIds);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
