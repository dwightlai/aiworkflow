package com.mw.ai.agi.auth.identity.adapter.model;

import java.util.List;

public record ExternalOrganizationProfile(
        String userId,
        List<String> organizationIds,
        String activeOrganizationId,
        List<String> unitIds,
        String activeUnitId,
        List<String> departmentIds,
        List<String> roleIds
) {
    public ExternalOrganizationProfile {
        organizationIds = organizationIds == null ? List.of() : List.copyOf(organizationIds);
        unitIds = unitIds == null ? List.of() : List.copyOf(unitIds);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
