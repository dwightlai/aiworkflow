package com.mw.ai.agi.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywaySchemaMigrationTest {
    private EmbeddedDatabase database;

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.shutdown();
        }
    }

    @Test
    void migratesBusinessTablesToAgiPrefix() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("agi-schema-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
                .build();
        Flyway.configure()
                .dataSource(database)
                .locations("classpath:db/migration/postgresql")
                .load()
                .migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(database);

        List<String> tableNames = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name
                """, String.class);

        assertThat(tableNames).containsExactlyInAnyOrder(
                "agi_workflow",
                "agi_workflow_version",
                "agi_workflow_execution",
                "agi_workflow_node_execution",
                "agi_integration_app",
                "agi_webhook_subscription",
                "agi_prompt_template",
                "agi_model_provider",
                "agi_vector_store_config",
                "agi_knowledge_base",
                "agi_knowledge_document",
                "agi_knowledge_chunk",
                "agi_knowledge_chunk_vector",
                "agi_ai_bot",
                "agi_bot_session",
                "agi_bot_message",
                "agi_tenant",
                "agi_organization",
                "agi_role",
                "agi_user",
                "agi_user_organization",
                "agi_user_role",
                "agi_integration_app_secret",
                "agi_integration_app_scope",
                "agi_login_session",
                "agi_auth_audit_log",
                "agi_asset_grant",
                "agi_generation_template",
                "agi_generation_job",
                "agi_generation_output"
        );
        assertThat(tableNames).allMatch(tableName -> tableName.startsWith("agi_"));

        List<String> organizationColumns = jdbcTemplate.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'agi_organization'
                """, String.class);
        assertThat(organizationColumns).contains("code", "external_org_id", "org_type", "parent_id", "path", "level");
        List<String> userColumns = jdbcTemplate.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'agi_user'
                """, String.class);
        assertThat(userColumns).contains("sort_order");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agi_user WHERE username = 'admin'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agi_organization WHERE id = 'org_default_unit'",
                Integer.class
        )).isEqualTo(1);
    }
}
