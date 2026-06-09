package com.mw.ai.agi.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class IntegrationAppScopeService {
    public static final String SCOPE_TENANT = "TENANT";
    public static final String SCOPE_ORGANIZATION = "ORGANIZATION";
    public static final String SCOPE_UNIT = "UNIT";
    public static final String SCOPE_BOT = "BOT";
    public static final String SCOPE_KNOWLEDGE_BASE = "KNOWLEDGE_BASE";
    public static final String SCOPE_WORKFLOW = "WORKFLOW";
    public static final String SCOPE_MODEL_PROVIDER = "MODEL_PROVIDER";
    public static final String SCOPE_TEMPLATE = "TEMPLATE";
    public static final String PERMISSION_USE = "USE";
    public static final String PERMISSION_MANAGE = "MANAGE";

    private final ObjectProvider<IntegrationAppScopeMapper> scopeMapperProvider;

    public IntegrationAppScopeService(ObjectProvider<IntegrationAppScopeMapper> scopeMapperProvider) {
        this.scopeMapperProvider = scopeMapperProvider;
    }

    public boolean allowsAllAssets(String appId) {
        return listEnabledScopes(appId).stream()
                .anyMatch(scope -> SCOPE_TENANT.equals(scope.getScopeType())
                        && PERMISSION_USE.equals(normalizePermission(scope.getPermission())));
    }

    public boolean hasAnyAssetScope(String appId) {
        return listEnabledScopes(appId).stream()
                .anyMatch(scope -> List.of(
                        SCOPE_BOT,
                        SCOPE_KNOWLEDGE_BASE,
                        SCOPE_WORKFLOW,
                        SCOPE_MODEL_PROVIDER,
                        SCOPE_TEMPLATE
                ).contains(scope.getScopeType())
                        && PERMISSION_USE.equals(normalizePermission(scope.getPermission())));
    }

    public boolean hasOrganizationScope(String appId, String tenantId, String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return allowsAllAssets(appId) || hasAnyAssetScope(appId);
        }
        return listEnabledScopes(appId).stream().anyMatch(scope -> {
            if (!List.of(PERMISSION_USE, PERMISSION_MANAGE).contains(normalizePermission(scope.getPermission()))) {
                return false;
            }
            if (SCOPE_TENANT.equals(scope.getScopeType()) && tenantId.equals(scope.getScopeId())) {
                return true;
            }
            if (List.of(SCOPE_ORGANIZATION, SCOPE_UNIT).contains(scope.getScopeType())
                    && organizationId.equals(scope.getScopeId())) {
                return true;
            }
            return false;
        });
    }

    public boolean isAssetAllowed(String appId, String assetScopeType, String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return false;
        }
        if (allowsAllAssets(appId)) {
            return true;
        }
        return listEnabledScopes(appId).stream().anyMatch(scope ->
                assetScopeType.equals(scope.getScopeType())
                        && assetId.equals(scope.getScopeId())
                        && PERMISSION_USE.equals(normalizePermission(scope.getPermission())));
    }

    public Set<String> listExplicitAssetIds(String appId, String assetScopeType) {
        Set<String> assetIds = new LinkedHashSet<>();
        for (IntegrationAppScopeEntity scope : listEnabledScopes(appId)) {
            if (assetScopeType.equals(scope.getScopeType())
                    && PERMISSION_USE.equals(normalizePermission(scope.getPermission()))
                    && scope.getScopeId() != null
                    && !scope.getScopeId().isBlank()) {
                assetIds.add(scope.getScopeId());
            }
        }
        return assetIds;
    }

    public void assertAssetAllowed(String appId, String assetScopeType, String assetId) {
        if (!isAssetAllowed(appId, assetScopeType, assetId)) {
            throw new AuthException(
                    "APP_ASSET_SCOPE_DENIED",
                    HttpStatus.FORBIDDEN,
                    "App cannot access " + assetScopeType + ": " + assetId
            );
        }
    }

    public List<IntegrationAppScopeEntity> listEnabledScopes(String appId) {
        return scopeMapper().selectList(new LambdaQueryWrapper<IntegrationAppScopeEntity>()
                .eq(IntegrationAppScopeEntity::getAppId, appId)
                .eq(IntegrationAppScopeEntity::getEnabled, true));
    }

    private String normalizePermission(String permission) {
        return permission == null || permission.isBlank() ? PERMISSION_USE : permission;
    }

    private IntegrationAppScopeMapper scopeMapper() {
        IntegrationAppScopeMapper mapper = scopeMapperProvider.getIfAvailable();
        if (mapper == null) {
            throw new AuthException(
                    "AUTH_STORAGE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "IntegrationAppScopeMapper is unavailable."
            );
        }
        return mapper;
    }
}
