package com.mw.ai.agi.auth.identity;

import com.mw.ai.agi.auth.service.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RemoteIdentityGuard {
    private final IdentityProperties identityProperties;

    public RemoteIdentityGuard(IdentityProperties identityProperties) {
        this.identityProperties = identityProperties;
    }

    public void assertLocalOrgAdminEnabled() {
        if (identityProperties.isRemote() && identityProperties.getRemote().isDisableLocalOrgAdmin()) {
            throw new AuthException(
                    "IDENTITY_MODE_REMOTE",
                    HttpStatus.FORBIDDEN,
                    "Organization and user administration is disabled in remote identity mode."
            );
        }
    }

    public void assertBreakGlassLoginAllowed() {
        if (identityProperties.isRemote() && !identityProperties.getRemote().isAllowBreakGlassLogin()) {
            throw new AuthException(
                    "IDENTITY_MODE_REMOTE",
                    HttpStatus.FORBIDDEN,
                    "Local login is disabled in remote identity mode."
            );
        }
    }
}
