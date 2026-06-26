package com.mw.ai.agi.auth.identity.adapter.model;

import java.util.List;

public record ExternalTokenIntrospection(
        boolean active,
        String userId,
        String username,
        List<String> roleIds,
        List<String> permissions,
        Long expiresAtEpochSeconds
) {
    public ExternalTokenIntrospection {
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
