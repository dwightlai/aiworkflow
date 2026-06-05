package com.mw.ai.agi.auth.service;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        AuthUserPrincipal user
) {
}
