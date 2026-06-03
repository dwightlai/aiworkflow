package com.aiworkflow.integration.organization;

public record OrganizationUserResponse(
        String id,
        String username,
        String displayName,
        String departmentId,
        String email,
        String mobile,
        boolean enabled
) {
}
