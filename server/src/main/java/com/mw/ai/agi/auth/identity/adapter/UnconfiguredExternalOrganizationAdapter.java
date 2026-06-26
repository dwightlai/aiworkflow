package com.mw.ai.agi.auth.identity.adapter;

import com.mw.ai.agi.auth.identity.adapter.model.ExternalOrganizationProfile;
import com.mw.ai.agi.auth.service.AuthException;
import org.springframework.http.HttpStatus;

public class UnconfiguredExternalOrganizationAdapter implements ExternalOrganizationAdapter {
    @Override
    public ExternalOrganizationProfile loadOrganizationContext(String userId) {
        throw new AuthException(
                "IDENTITY_ADAPTER_NOT_CONFIGURED",
                HttpStatus.SERVICE_UNAVAILABLE,
                "External organization adapter is not configured. Implement ExternalOrganizationAdapter and register it as a Spring bean."
        );
    }
}
