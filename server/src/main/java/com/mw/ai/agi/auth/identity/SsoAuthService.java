package com.mw.ai.agi.auth.identity;

import com.mw.ai.agi.auth.service.AuthException;
import com.mw.ai.agi.auth.service.AuthService;
import com.mw.ai.agi.auth.service.AuthTokenResponse;
import com.mw.ai.agi.auth.service.AuthUserPrincipal;
import com.mw.ai.agi.auth.service.RequestAuditContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SsoAuthService {
    private final IdentityProperties identityProperties;
    private final ExternalIdentityResolver externalIdentityResolver;
    private final AuthService authService;

    public SsoAuthService(
            IdentityProperties identityProperties,
            ExternalIdentityResolver externalIdentityResolver,
            AuthService authService
    ) {
        this.identityProperties = identityProperties;
        this.externalIdentityResolver = externalIdentityResolver;
        this.authService = authService;
    }

    public AuthTokenResponse exchange(String externalToken, RequestAuditContext auditContext) {
        assertRemoteMode();
        AuthUserPrincipal principal = externalIdentityResolver.resolveFromExternalToken(externalToken);
        return authService.issueTokenForPrincipal(principal, auditContext, "SSO_EXCHANGE");
    }

    public IdentityModeView currentMode() {
        return new IdentityModeView(
                identityProperties.resolvedMode().name().toLowerCase(),
                identityProperties.getFixedTenantId(),
                identityProperties.getRemote().isAllowBreakGlassLogin(),
                identityProperties.getRemote().isDisableLocalOrgAdmin()
        );
    }

    private void assertRemoteMode() {
        if (!identityProperties.isRemote()) {
            throw new AuthException(
                    "IDENTITY_MODE_LOCAL",
                    HttpStatus.BAD_REQUEST,
                    "SSO exchange is only available when agi.identity.mode=remote."
            );
        }
    }

    public record IdentityModeView(
            String mode,
            String fixedTenantId,
            boolean allowBreakGlassLogin,
            boolean disableLocalOrgAdmin
    ) {
    }
}
