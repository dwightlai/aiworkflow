package com.mw.ai.agi.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IntegrationAppAuthenticator {
    private final ObjectProvider<IntegrationAppMapper> integrationAppMapperProvider;
    private final ObjectProvider<IntegrationAppSecretMapper> integrationAppSecretMapperProvider;
    private final ObjectProvider<IntegrationAppScopeMapper> integrationAppScopeMapperProvider;
    private final SecretHasher secretHasher;
    private final AuthService authService;

    public IntegrationAppAuthenticator(
            ObjectProvider<IntegrationAppMapper> integrationAppMapperProvider,
            ObjectProvider<IntegrationAppSecretMapper> integrationAppSecretMapperProvider,
            ObjectProvider<IntegrationAppScopeMapper> integrationAppScopeMapperProvider,
            SecretHasher secretHasher,
            AuthService authService
    ) {
        this.integrationAppMapperProvider = integrationAppMapperProvider;
        this.integrationAppSecretMapperProvider = integrationAppSecretMapperProvider;
        this.integrationAppScopeMapperProvider = integrationAppScopeMapperProvider;
        this.secretHasher = secretHasher;
        this.authService = authService;
    }

    public RuntimeIdentityContext authenticateApiKey(
            String appCode,
            String apiKey,
            ExternalCallerContext callerContext,
            RequestAuditContext auditContext
    ) {
        IntegrationAppEntity app = findApp(appCode)
                .orElseThrow(() -> new AuthException("APP_NOT_FOUND", HttpStatus.UNAUTHORIZED, "Integration app does not exist."));
        if (!"ACTIVE".equals(app.getStatus())) {
            auditDenied(app, callerContext, auditContext, "APP_DISABLED");
            throw new AuthException("APP_DISABLED", HttpStatus.FORBIDDEN, "Integration app is disabled.");
        }
        if (!hasValidSecret(app.getId(), apiKey)) {
            auditDenied(app, callerContext, auditContext, "APP_SECRET_INVALID");
            throw new AuthException("APP_SECRET_INVALID", HttpStatus.UNAUTHORIZED, "API key is invalid.");
        }
        if (callerContext.unitId() == null || callerContext.unitId().isBlank()) {
            auditDenied(app, callerContext, auditContext, "IDENTITY_CONTEXT_MISSING");
            throw new AuthException("IDENTITY_CONTEXT_MISSING", HttpStatus.BAD_REQUEST, "Unit context is required.");
        }
        if (!hasUnitScope(app.getId(), app.getTenantId(), callerContext.unitId())) {
            auditDenied(app, callerContext, auditContext, "APP_UNIT_SCOPE_DENIED");
            throw new AuthException("APP_UNIT_SCOPE_DENIED", HttpStatus.FORBIDDEN, "App cannot represent this unit.");
        }

        RuntimeIdentityContext context = new RuntimeIdentityContext(
                app.getTenantId(),
                app.getId(),
                callerContext.userId(),
                List.of(callerContext.unitId()),
                callerContext.unitId(),
                callerContext.departmentIds(),
                callerContext.roleIds(),
                "API_KEY",
                app.getCode()
        );
        authService.audit("API_KEY_USED", app.getTenantId(), callerContext.userId(), app.getId(), callerContext.unitId(),
                callerContext.departmentIds(), callerContext.roleIds(), auditContext, "SUCCESS", null);
        return context;
    }

    private Optional<IntegrationAppEntity> findApp(String appCode) {
        return Optional.ofNullable(integrationAppMapper().selectOne(new LambdaQueryWrapper<IntegrationAppEntity>()
                .eq(IntegrationAppEntity::getCode, appCode)
                .eq(IntegrationAppEntity::getAuthType, "API_KEY")));
    }

    private boolean hasValidSecret(String appId, String apiKey) {
        return integrationAppSecretMapper().selectList(new LambdaQueryWrapper<IntegrationAppSecretEntity>()
                        .eq(IntegrationAppSecretEntity::getAppId, appId)
                        .eq(IntegrationAppSecretEntity::getEnabled, true)
                        .and(wrapper -> wrapper
                                .isNull(IntegrationAppSecretEntity::getExpiresAt)
                                .or()
                                .gt(IntegrationAppSecretEntity::getExpiresAt, Instant.now())))
                .stream()
                .anyMatch(secret -> secretHasher.matches(apiKey, secret.getSecretHash()));
    }

    private boolean hasUnitScope(String appId, String tenantId, String unitId) {
        return integrationAppScopeMapper().selectList(new LambdaQueryWrapper<IntegrationAppScopeEntity>()
                        .eq(IntegrationAppScopeEntity::getAppId, appId)
                        .eq(IntegrationAppScopeEntity::getEnabled, true)
                        .in(IntegrationAppScopeEntity::getPermission, List.of("USE", "MANAGE"))
                        .and(wrapper -> wrapper
                                .eq(IntegrationAppScopeEntity::getScopeType, "UNIT")
                                .eq(IntegrationAppScopeEntity::getScopeId, unitId)
                                .or(nested -> nested
                                        .eq(IntegrationAppScopeEntity::getScopeType, "TENANT")
                                        .eq(IntegrationAppScopeEntity::getScopeId, tenantId))))
                .stream()
                .findAny()
                .isPresent();
    }

    private void auditDenied(IntegrationAppEntity app, ExternalCallerContext callerContext, RequestAuditContext auditContext, String errorCode) {
        authService.audit("APP_DENIED", app.getTenantId(), callerContext.userId(), app.getId(), callerContext.unitId(),
                callerContext.departmentIds(), callerContext.roleIds(), auditContext, "FAILED", errorCode);
    }

    private IntegrationAppMapper integrationAppMapper() {
        return required(integrationAppMapperProvider, IntegrationAppMapper.class);
    }

    private IntegrationAppSecretMapper integrationAppSecretMapper() {
        return required(integrationAppSecretMapperProvider, IntegrationAppSecretMapper.class);
    }

    private IntegrationAppScopeMapper integrationAppScopeMapper() {
        return required(integrationAppScopeMapperProvider, IntegrationAppScopeMapper.class);
    }

    private <T> T required(ObjectProvider<T> provider, Class<T> type) {
        T mapper = provider.getIfAvailable();
        if (mapper == null) {
            throw new AuthException("AUTH_STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    type.getSimpleName() + " is unavailable.");
        }
        return mapper;
    }
}
