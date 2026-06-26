package com.mw.ai.agi.auth.identity;

import com.mw.ai.agi.auth.identity.adapter.ExternalAuthAdapter;
import com.mw.ai.agi.auth.identity.adapter.ExternalOrganizationAdapter;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalOrganizationProfile;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalTokenIntrospection;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalUserProfile;
import com.mw.ai.agi.auth.service.AuthException;
import com.mw.ai.agi.auth.service.AuthUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExternalIdentityResolver {
    private final IdentityProperties identityProperties;
    private final ExternalAuthAdapter authAdapter;
    private final ExternalOrganizationAdapter organizationAdapter;

    public ExternalIdentityResolver(
            IdentityProperties identityProperties,
            ExternalAuthAdapter authAdapter,
            ExternalOrganizationAdapter organizationAdapter
    ) {
        this.identityProperties = identityProperties;
        this.authAdapter = authAdapter;
        this.organizationAdapter = organizationAdapter;
    }

    public AuthUserPrincipal resolveFromExternalToken(String externalToken) {
        ExternalTokenIntrospection introspection = authAdapter.introspect(externalToken);
        if (!introspection.active()) {
            throw new AuthException("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "External token is inactive.");
        }
        if (introspection.userId() == null || introspection.userId().isBlank()) {
            throw new AuthException("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "External token has no user id.");
        }
        ExternalUserProfile userProfile = authAdapter.getUser(introspection.userId())
                .orElseGet(() -> fallbackUserProfile(introspection));
        if (!userProfile.enabled()) {
            throw new AuthException("AUTH_USER_DISABLED", HttpStatus.FORBIDDEN, "External user is disabled.");
        }
        ExternalOrganizationProfile organizationProfile = organizationAdapter.loadOrganizationContext(introspection.userId());
        List<String> mergedRoles = mergeRoleIds(introspection.roleIds(), userProfile.roleIds(), organizationProfile.roleIds());
        List<String> mappedRoles = identityProperties.mapRoleIds(mergedRoles);
        return new AuthUserPrincipal(
                userProfile.userId(),
                firstNonBlank(userProfile.username(), introspection.username()),
                identityProperties.getFixedTenantId(),
                firstNonBlank(userProfile.displayName(), userProfile.username(), introspection.username()),
                "EXTERNAL",
                0,
                organizationProfile.organizationIds(),
                organizationProfile.activeOrganizationId(),
                organizationProfile.unitIds(),
                organizationProfile.activeUnitId(),
                organizationProfile.departmentIds(),
                mappedRoles
        );
    }

    private ExternalUserProfile fallbackUserProfile(ExternalTokenIntrospection introspection) {
        return new ExternalUserProfile(
                introspection.userId(),
                introspection.username(),
                introspection.username(),
                true,
                introspection.roleIds()
        );
    }

    private List<String> mergeRoleIds(List<String>... roleLists) {
        Set<String> merged = new LinkedHashSet<>();
        for (List<String> roleIds : roleLists) {
            if (roleIds != null) {
                roleIds.stream().filter(role -> role != null && !role.isBlank()).forEach(merged::add);
            }
        }
        return new ArrayList<>(merged);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
