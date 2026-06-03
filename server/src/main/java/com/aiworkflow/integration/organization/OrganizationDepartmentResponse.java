package com.aiworkflow.integration.organization;

public record OrganizationDepartmentResponse(
        String id,
        String parentId,
        String name,
        String code,
        boolean enabled
) {
}
