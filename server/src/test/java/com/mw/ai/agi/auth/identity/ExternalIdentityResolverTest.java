package com.mw.ai.agi.auth.identity;

import com.mw.ai.agi.auth.identity.adapter.ExternalAuthAdapter;
import com.mw.ai.agi.auth.identity.adapter.ExternalOrganizationAdapter;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalOrganizationProfile;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalTokenIntrospection;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalUserProfile;
import com.mw.ai.agi.auth.service.AuthUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalIdentityResolverTest {
    private final ExternalAuthAdapter authAdapter = mock(ExternalAuthAdapter.class);
    private final ExternalOrganizationAdapter organizationAdapter = mock(ExternalOrganizationAdapter.class);
    private IdentityProperties identityProperties;
    private ExternalIdentityResolver resolver;

    @BeforeEach
    void setUp() {
        identityProperties = new IdentityProperties();
        identityProperties.setMode("remote");
        identityProperties.setFixedTenantId("tenant_default");
        identityProperties.setRoleMapping(Map.of("archive_admin", "asset_manager"));
        resolver = new ExternalIdentityResolver(identityProperties, authAdapter, organizationAdapter);
    }

    @Test
    void shouldResolveExternalPrincipalWithMappedRoles() {
        when(authAdapter.introspect("ext-token")).thenReturn(new ExternalTokenIntrospection(
                true, "u_001", "zhangsan", List.of("archive_admin"), List.of(), null
        ));
        when(authAdapter.getUser("u_001")).thenReturn(Optional.of(new ExternalUserProfile(
                "u_001", "zhangsan", "张三", true, List.of("archive_admin")
        )));
        when(organizationAdapter.loadOrganizationContext("u_001")).thenReturn(new ExternalOrganizationProfile(
                "u_001", List.of("org_001"), "org_001", List.of("unit_001"), "unit_001", List.of("dept_001"), List.of()
        ));

        AuthUserPrincipal principal = resolver.resolveFromExternalToken("ext-token");

        assertThat(principal.id()).isEqualTo("u_001");
        assertThat(principal.tenantId()).isEqualTo("tenant_default");
        assertThat(principal.userType()).isEqualTo("EXTERNAL");
        assertThat(principal.roleIds()).containsExactly("asset_manager");
        assertThat(principal.activeUnitId()).isEqualTo("unit_001");
    }

    @Test
    void shouldFallbackToIntrospectionWhenUserProfileMissing() {
        when(authAdapter.introspect(anyString())).thenReturn(new ExternalTokenIntrospection(
                true, "u_002", "lisi", List.of("archive_user"), List.of(), null
        ));
        when(authAdapter.getUser("u_002")).thenReturn(Optional.empty());
        when(organizationAdapter.loadOrganizationContext("u_002")).thenReturn(new ExternalOrganizationProfile(
                "u_002", List.of(), null, List.of(), null, List.of(), List.of()
        ));

        AuthUserPrincipal principal = resolver.resolveFromExternalToken("token");

        assertThat(principal.username()).isEqualTo("lisi");
        assertThat(principal.roleIds()).containsExactly("archive_user");
    }
}
