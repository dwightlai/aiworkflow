package com.mw.ai.agi.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.persistence.AuthAuditLogEntity;
import com.mw.ai.agi.auth.persistence.AuthAuditLogMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:integration-app-auth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false"
})
class IntegrationAppAuthenticatorTest {
    @Autowired
    private IntegrationAppMapper integrationAppMapper;

    @Autowired
    private IntegrationAppSecretMapper integrationAppSecretMapper;

    @Autowired
    private IntegrationAppScopeMapper integrationAppScopeMapper;

    @Autowired
    private AuthAuditLogMapper authAuditLogMapper;

    @Autowired
    private IntegrationAppAuthenticator authenticator;

    @Autowired
    private SecretHasher secretHasher;

    @Test
    void apiKeyBuildsRuntimeIdentityWhenUnitScopeMatches() {
        insertArchiveApp("agi_test_key", "unit_archive");

        RuntimeIdentityContext context = authenticator.authenticateApiKey(
                "archive-system",
                "agi_test_key",
                new ExternalCallerContext(
                        "unit_archive",
                        List.of("dept_archive"),
                        List.of("archive_user", "department_leader"),
                        "external_user_001"
                ),
                new RequestAuditContext("127.0.0.1", "JUnit")
        );

        assertThat(context.tenantId()).isEqualTo("tenant_default");
        assertThat(context.appId()).isEqualTo("app_archive");
        assertThat(context.userId()).isEqualTo("external_user_001");
        assertThat(context.activeUnitId()).isEqualTo("unit_archive");
        assertThat(context.unitIds()).containsExactly("unit_archive");
        assertThat(context.departmentIds()).containsExactly("dept_archive");
        assertThat(context.roleIds()).containsExactly("archive_user", "department_leader");
        assertThat(context.authType()).isEqualTo("API_KEY");

        assertThat(authAuditLogMapper.selectCount(new LambdaQueryWrapper<AuthAuditLogEntity>()
                .eq(AuthAuditLogEntity::getEventType, "API_KEY_USED")
                .eq(AuthAuditLogEntity::getResult, "SUCCESS"))).isEqualTo(1);
    }

    @Test
    void apiKeyRejectsUnitOutsideAppScope() {
        insertArchiveApp("agi_test_key", "unit_archive");

        assertThatThrownBy(() -> authenticator.authenticateApiKey(
                "archive-system",
                "agi_test_key",
                new ExternalCallerContext("unit_other", List.of(), List.of("archive_user"), "external_user_001"),
                new RequestAuditContext("127.0.0.1", "JUnit")
        ))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("APP_UNIT_SCOPE_DENIED");

        assertThat(authAuditLogMapper.selectCount(new LambdaQueryWrapper<AuthAuditLogEntity>()
                .eq(AuthAuditLogEntity::getEventType, "APP_DENIED")
                .eq(AuthAuditLogEntity::getErrorCode, "APP_UNIT_SCOPE_DENIED"))).isEqualTo(1);
    }

    private void insertArchiveApp(String apiKey, String unitScope) {
        Instant now = Instant.parse("2026-06-05T00:00:00Z");
        authAuditLogMapper.delete(new LambdaQueryWrapper<AuthAuditLogEntity>().eq(AuthAuditLogEntity::getAppId, "app_archive"));
        integrationAppScopeMapper.delete(new LambdaQueryWrapper<IntegrationAppScopeEntity>().eq(IntegrationAppScopeEntity::getAppId, "app_archive"));
        integrationAppSecretMapper.delete(new LambdaQueryWrapper<IntegrationAppSecretEntity>().eq(IntegrationAppSecretEntity::getAppId, "app_archive"));
        integrationAppMapper.deleteById("app_archive");

        IntegrationAppEntity app = new IntegrationAppEntity();
        app.setId("app_archive");
        app.setTenantId("tenant_default");
        app.setCode("archive-system");
        app.setName("Archive System");
        app.setAppType("ARCHIVE_SYSTEM");
        app.setAuthType("API_KEY");
        app.setStatus("ACTIVE");
        app.setCreatedAt(now);
        app.setUpdatedAt(now);
        integrationAppMapper.insert(app);

        IntegrationAppSecretEntity secret = new IntegrationAppSecretEntity();
        secret.setId("secret_archive");
        secret.setTenantId("tenant_default");
        secret.setAppId("app_archive");
        secret.setSecretHash(secretHasher.hash(apiKey));
        secret.setSecretPrefix("agi_test");
        secret.setEnabled(true);
        secret.setCreatedAt(now);
        secret.setUpdatedAt(now);
        integrationAppSecretMapper.insert(secret);

        IntegrationAppScopeEntity scope = new IntegrationAppScopeEntity();
        scope.setId("scope_archive_unit");
        scope.setTenantId("tenant_default");
        scope.setAppId("app_archive");
        scope.setScopeType("UNIT");
        scope.setScopeId(unitScope);
        scope.setPermission("USE");
        scope.setEnabled(true);
        scope.setCreatedAt(now);
        scope.setUpdatedAt(now);
        integrationAppScopeMapper.insert(scope);
    }
}
