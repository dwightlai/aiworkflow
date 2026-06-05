package com.mw.ai.agi.config;

import com.mw.ai.agi.auth.persistence.AuthAuditLogEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretEntity;
import com.mw.ai.agi.auth.persistence.LoginSessionEntity;
import com.mw.ai.agi.auth.persistence.RoleEntity;
import com.mw.ai.agi.auth.persistence.DepartmentEntity;
import com.mw.ai.agi.auth.persistence.TenantEntity;
import com.mw.ai.agi.auth.persistence.UnitEntity;
import com.mw.ai.agi.auth.persistence.UserDepartmentEntity;
import com.mw.ai.agi.auth.persistence.UserEntity;
import com.mw.ai.agi.auth.persistence.UserRoleEntity;
import com.mw.ai.agi.auth.persistence.UserUnitEntity;
import com.mw.ai.agi.bot.persistence.AiBotEntity;
import com.mw.ai.agi.bot.persistence.BotMessageEntity;
import com.mw.ai.agi.bot.persistence.BotSessionEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeBaseEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeChunkEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeChunkVectorEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeDocumentEntity;
import com.mw.ai.agi.knowledge.persistence.VectorStoreConfigEntity;
import com.mw.ai.agi.model.persistence.ModelProviderEntity;
import com.mw.ai.agi.prompt.persistence.PromptTemplateEntity;
import com.mw.ai.agi.workflow.persistence.WorkflowEntity;
import com.mw.ai.agi.workflow.persistence.WorkflowExecutionEntity;
import com.mw.ai.agi.workflow.persistence.WorkflowNodeExecutionEntity;
import com.mw.ai.agi.workflow.persistence.WorkflowVersionEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DamengSchemaMigrationContractTest {
    private static final List<Class<?>> PERSISTENCE_ENTITIES = List.of(
            AiBotEntity.class,
            BotMessageEntity.class,
            BotSessionEntity.class,
            KnowledgeBaseEntity.class,
            KnowledgeChunkEntity.class,
            KnowledgeChunkVectorEntity.class,
            KnowledgeDocumentEntity.class,
            VectorStoreConfigEntity.class,
            ModelProviderEntity.class,
            PromptTemplateEntity.class,
            WorkflowEntity.class,
            WorkflowExecutionEntity.class,
            WorkflowNodeExecutionEntity.class,
            WorkflowVersionEntity.class,
            AuthAuditLogEntity.class,
            IntegrationAppEntity.class,
            IntegrationAppScopeEntity.class,
            IntegrationAppSecretEntity.class,
            LoginSessionEntity.class,
            RoleEntity.class,
            DepartmentEntity.class,
            TenantEntity.class,
            UnitEntity.class,
            UserDepartmentEntity.class,
            UserEntity.class,
            UserRoleEntity.class,
            UserUnitEntity.class
    );

    private static final List<String> POSTGRES_ONLY_PATTERNS = List.of(
            "IF NOT EXISTS",
            "TIMESTAMP WITH TIME ZONE",
            " BOOLEAN",
            "ADD COLUMN IF NOT EXISTS",
            "ALTER COLUMN",
            " RENAME TO "
    );

    @Test
    void damengMigrationDefinesEveryMybatisTable() throws Exception {
        String sql = damengSql().toLowerCase(Locale.ROOT);

        List<String> tableNames = PERSISTENCE_ENTITIES.stream()
                .map(entity -> entity.getAnnotation(TableName.class).value())
                .toList();

        assertThat(tableNames).allSatisfy(tableName ->
                assertThat(sql).contains("create table " + tableName.toLowerCase(Locale.ROOT)));

        assertThat(sql).contains(
                "create table agi_tenant",
                "create table agi_unit",
                "create table agi_department",
                "create table agi_role",
                "create table agi_user",
                "create table agi_user_unit",
                "create table agi_user_department",
                "create table agi_user_role",
                "create table agi_integration_app_secret",
                "create table agi_integration_app_scope",
                "create table agi_login_session",
                "create table agi_auth_audit_log",
                "code varchar(100) not null",
                "external_unit_id varchar(200)",
                "external_department_id varchar(200)"
        );
    }

    @Test
    void damengMigrationUsesNumberOneForBooleanColumns() throws Exception {
        String sql = damengSql().toLowerCase(Locale.ROOT);

        assertThat(sql).doesNotContain(" boolean");
        assertThat(sql).containsPattern(Pattern.compile("\\benabled\\s+number\\(1\\)\\s+not\\s+null"));
        assertThat(sql).containsPattern(Pattern.compile("\\bvision_support\\s+number\\(1\\)\\s+not\\s+null"));
    }

    @Test
    void damengMigrationAvoidsPostgresqlOnlySyntax() throws Exception {
        String sql = damengSql().toUpperCase(Locale.ROOT);

        assertThat(POSTGRES_ONLY_PATTERNS)
                .allSatisfy(pattern -> assertThat(sql).doesNotContain(pattern));
    }

    private String damengSql() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath:db/migration/dameng/*.sql");
        assertThat(resources).isNotEmpty();

        StringBuilder builder = new StringBuilder();
        for (var resource : resources) {
            builder.append(resource.getContentAsString(StandardCharsets.UTF_8)).append('\n');
        }
        return builder.toString();
    }
}
