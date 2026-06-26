package com.mw.ai.agi.auth.identity;

import com.mw.ai.agi.auth.service.AuthException;
import com.mw.ai.agi.auth.service.AuthService;
import com.mw.ai.agi.auth.service.AuthTokenResponse;
import com.mw.ai.agi.auth.service.AuthUserPrincipal;
import com.mw.ai.agi.auth.service.RequestAuditContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SsoAuthServiceTest {
    private IdentityProperties identityProperties;
    private ExternalIdentityResolver externalIdentityResolver;
    private AuthService authService;
    private SsoAuthService ssoAuthService;

    @BeforeEach
    void setUp() {
        identityProperties = new IdentityProperties();
        externalIdentityResolver = mock(ExternalIdentityResolver.class);
        authService = mock(AuthService.class);
        ssoAuthService = new SsoAuthService(identityProperties, externalIdentityResolver, authService);
    }

    @Test
    void shouldRejectSsoExchangeInLocalMode() {
        identityProperties.setMode("local");

        assertThatThrownBy(() -> ssoAuthService.exchange("token", new RequestAuditContext("127.0.0.1", "junit")))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo("IDENTITY_MODE_LOCAL");
    }

    @Test
    void shouldExchangeExternalTokenInRemoteMode() {
        identityProperties.setMode("remote");
        AuthUserPrincipal principal = new AuthUserPrincipal(
                "u_001", "zhangsan", "tenant_default", "张三", "EXTERNAL", 0,
                List.of(), null, List.of("unit_001"), "unit_001", List.of(), List.of("asset_manager")
        );
        AuthTokenResponse tokenResponse = new AuthTokenResponse("access", "refresh", 3600L, principal);
        when(externalIdentityResolver.resolveFromExternalToken("ext-token")).thenReturn(principal);
        when(authService.issueTokenForPrincipal(eq(principal), any(), eq("SSO_EXCHANGE"))).thenReturn(tokenResponse);

        AuthTokenResponse response = ssoAuthService.exchange("ext-token", new RequestAuditContext("127.0.0.1", "junit"));

        assertThat(response.accessToken()).isEqualTo("access");
        verify(authService).issueTokenForPrincipal(eq(principal), any(), eq("SSO_EXCHANGE"));
    }
}
