package com.mw.ai.agi.auth.identity.adapter.model;

import java.util.List;

public record ExternalUserProfile(
        String userId,
        String username,
        String displayName,
        boolean enabled,
        List<String> roleIds
) {
    public ExternalUserProfile {
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
