package com.aiworkflow.integration.auth;

import java.util.List;

public record AuthUserResponse(
        String id,
        String username,
        String displayName,
        List<String> roles,
        boolean enabled
) {
    public AuthUserResponse {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
