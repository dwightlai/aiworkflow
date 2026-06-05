package com.mw.ai.agi.integration.organization;

public record OrganizationDepartmentResponse(
        String id,
        String parentId,
        String name,
        String code,
        boolean enabled
) {
}
