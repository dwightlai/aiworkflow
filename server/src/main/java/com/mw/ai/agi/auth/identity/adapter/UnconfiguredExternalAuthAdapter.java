package com.mw.ai.agi.auth.identity.adapter;

import com.mw.ai.agi.auth.identity.adapter.model.ExternalTokenIntrospection;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalUserProfile;
import com.mw.ai.agi.auth.service.AuthException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

public class UnconfiguredExternalAuthAdapter implements ExternalAuthAdapter {
    @Override
    public ExternalTokenIntrospection introspect(String accessToken) {
        throw notConfigured();
    }

    @Override
    public Optional<ExternalUserProfile> getUser(String userId) {
        throw notConfigured();
    }

    private AuthException notConfigured() {
        return new AuthException(
                "IDENTITY_ADAPTER_NOT_CONFIGURED",
                HttpStatus.SERVICE_UNAVAILABLE,
                "External auth adapter is not configured. Implement ExternalAuthAdapter and register it as a Spring bean."
        );
    }
}
