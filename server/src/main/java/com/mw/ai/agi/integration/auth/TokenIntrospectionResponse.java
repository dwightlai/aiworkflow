package com.mw.ai.agi.integration.auth;

import java.util.List;

public record TokenIntrospectionResponse(
        boolean active,
        String userId,
        String username,
        List<String> roles,
        List<String> permissions
) {
    public TokenIntrospectionResponse {
        roles = roles == null ? List.of() : List.copyOf(roles);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
