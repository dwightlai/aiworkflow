package com.mw.ai.agi.integration.organization;

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
