# Dameng and PostgreSQL Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Dameng SQL/configuration support while keeping PostgreSQL as the default persistence backend.

**Architecture:** Split Flyway migrations by database vendor and let Spring configuration select the migration location. Keep MyBatis-Plus store code unchanged unless `NUMBER(1)` Boolean mapping proves it needs a type handler in a real Dameng integration run.

**Tech Stack:** Java 17, Spring Boot 3.3.5, MyBatis-Plus 3.5.9, Flyway, Maven, JUnit 5, AssertJ.

---

## File Structure

- Modify `server/src/main/resources/application.yml`: make datasource and Flyway properties environment-variable driven, defaulting to PostgreSQL.
- Create `server/src/main/resources/application-dameng.yml`: example Dameng profile that expects the runtime to provide `dm.jdbc.driver.DmDriver`.
- Move `server/src/main/resources/db/migration/V*.sql` into `server/src/main/resources/db/migration/postgresql/`: preserve current PostgreSQL migration history.
- Create `server/src/main/resources/db/migration/dameng/V1__agi_schema.sql`: create the final `agi_*` tables directly for new Dameng deployments.
- Modify `server/src/test/java/com/aiworkflow/config/FlywaySchemaMigrationTest.java`: point the migration test at `classpath:db/migration/postgresql`.
- Create `server/src/test/java/com/aiworkflow/config/DamengSchemaMigrationContractTest.java`: static checks for Dameng SQL table coverage, `NUMBER(1)` Boolean fields, and absence of PostgreSQL-only syntax.
- Modify `server/src/test/java/com/aiworkflow/persistence/MybatisPersistenceIntegrationTest.java`: set Flyway location to the PostgreSQL folder for current H2 regression tests.

---

### Task 1: PostgreSQL Migration Location Regression

**Files:**
- Modify: `server/src/test/java/com/aiworkflow/config/FlywaySchemaMigrationTest.java`
- Modify: `server/src/test/java/com/aiworkflow/persistence/MybatisPersistenceIntegrationTest.java`

- [ ] **Step 1: Write the failing test changes**

In `server/src/test/java/com/aiworkflow/config/FlywaySchemaMigrationTest.java`, change:

```java
Flyway.configure()
        .dataSource(database)
        .locations("classpath:db/migration")
        .load()
        .migrate();
```

to:

```java
Flyway.configure()
        .dataSource(database)
        .locations("classpath:db/migration/postgresql")
        .load()
        .migrate();
```

In `server/src/test/java/com/aiworkflow/persistence/MybatisPersistenceIntegrationTest.java`, add this Spring Boot test property:

```java
"spring.flyway.locations=classpath:db/migration/postgresql",
```

The annotation should become:

```java
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mybatis-persistence;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false"
})
```

- [ ] **Step 2: Run tests to verify they fail before moving files**

Run:

```bash
cd server
mvn -Dtest=FlywaySchemaMigrationTest,MybatisPersistenceIntegrationTest test
```

Expected: tests fail because `classpath:db/migration/postgresql` does not exist yet or has no migrations.

- [ ] **Step 3: Move PostgreSQL migration files**

Create `server/src/main/resources/db/migration/postgresql/`.

Move all current migration files from `server/src/main/resources/db/migration/` into `server/src/main/resources/db/migration/postgresql/`:

```text
V1__foundation_schema.sql
V2__ai_studio_schema.sql
V3__knowledge_schema.sql
V4__model_provider_columns.sql
V5__model_provider_usage.sql
V6__knowledge_chunk_vectors.sql
V7__knowledge_base_vector_dimension.sql
V8__ai_bot_schema.sql
V9__bot_conversation_schema.sql
V10__vector_store_es_connection.sql
V11__bot_optional_workflow.sql
V12__agi_table_prefix.sql
```

- [ ] **Step 4: Run tests to verify PostgreSQL path passes**

Run:

```bash
cd server
mvn -Dtest=FlywaySchemaMigrationTest,MybatisPersistenceIntegrationTest test
```

Expected: both tests pass and still produce the final `agi_*` tables.

- [ ] **Step 5: Commit**

```bash
git add server/src/main/resources/db/migration server/src/test/java/com/aiworkflow/config/FlywaySchemaMigrationTest.java server/src/test/java/com/aiworkflow/persistence/MybatisPersistenceIntegrationTest.java
git commit -m "test: split postgresql migration location"
```

---

### Task 2: Configurable PostgreSQL and Dameng Profiles

**Files:**
- Modify: `server/src/main/resources/application.yml`
- Create: `server/src/main/resources/application-dameng.yml`

- [ ] **Step 1: Write the failing configuration assertion test**

Create `server/src/test/java/com/aiworkflow/config/DatabaseConfigurationTest.java`:

```java
package com.aiworkflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigurationTest {
    @Test
    void defaultApplicationConfigurationKeepsPostgresqlMigrationLocation() throws Exception {
        PropertySourcesPropertyResolver resolver = resolverFor("application.yml", Map.of());

        assertThat(resolver.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://localhost:5432/aiworkflow");
        assertThat(resolver.getProperty("spring.datasource.username")).isEqualTo("aiworkflow");
        assertThat(resolver.getProperty("spring.datasource.password")).isEqualTo("aiworkflow");
        assertThat(resolver.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/postgresql");
    }

    @Test
    void defaultApplicationConfigurationAcceptsDatasourceOverrides() throws Exception {
        PropertySourcesPropertyResolver resolver = resolverFor("application.yml", Map.of(
                "POSTGRES_JDBC_URL", "jdbc:postgresql://db.example:5432/custom",
                "POSTGRES_USERNAME", "custom_user",
                "POSTGRES_PASSWORD", "custom_password",
                "FLYWAY_LOCATIONS", "classpath:custom/location"
        ));

        assertThat(resolver.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.example:5432/custom");
        assertThat(resolver.getProperty("spring.datasource.username")).isEqualTo("custom_user");
        assertThat(resolver.getProperty("spring.datasource.password")).isEqualTo("custom_password");
        assertThat(resolver.getProperty("spring.flyway.locations")).isEqualTo("classpath:custom/location");
    }

    @Test
    void damengProfilePointsAtDamengDriverAndMigrationLocation() throws Exception {
        PropertySourcesPropertyResolver resolver = resolverFor("application-dameng.yml", Map.of());

        assertThat(resolver.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:dm://localhost:5236/AIWORKFLOW");
        assertThat(resolver.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("dm.jdbc.driver.DmDriver");
        assertThat(resolver.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/dameng");
    }

    private PropertySourcesPropertyResolver resolverFor(String resourceName, Map<String, Object> overrides) throws Exception {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("test-overrides", overrides));
        propertySources.addLast(new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName))
                .getFirst());
        return new PropertySourcesPropertyResolver(propertySources);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd server
mvn -Dtest=DatabaseConfigurationTest test
```

Expected: fail because `application-dameng.yml` does not exist and `application.yml` still points Flyway at `classpath:db/migration`.

- [ ] **Step 3: Update default application configuration**

In `server/src/main/resources/application.yml`, replace datasource and Flyway values with:

```yaml
  datasource:
    url: ${POSTGRES_JDBC_URL:jdbc:postgresql://localhost:5432/aiworkflow}
    username: ${POSTGRES_USERNAME:aiworkflow}
    password: ${POSTGRES_PASSWORD:aiworkflow}
  flyway:
    enabled: true
    locations: ${FLYWAY_LOCATIONS:classpath:db/migration/postgresql}
```

- [ ] **Step 4: Add Dameng profile**

Create `server/src/main/resources/application-dameng.yml`:

```yaml
spring:
  datasource:
    url: ${DAMENG_JDBC_URL:jdbc:dm://localhost:5236/AIWORKFLOW}
    username: ${DAMENG_USERNAME:SYSDBA}
    password: ${DAMENG_PASSWORD:SYSDBA}
    driver-class-name: ${DAMENG_DRIVER_CLASS_NAME:dm.jdbc.driver.DmDriver}
  flyway:
    locations: classpath:db/migration/dameng
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```bash
cd server
mvn -Dtest=DatabaseConfigurationTest test
```

Expected: pass.

- [ ] **Step 6: Commit**

```bash
git add server/src/main/resources/application.yml server/src/main/resources/application-dameng.yml server/src/test/java/com/aiworkflow/config/DatabaseConfigurationTest.java
git commit -m "feat: add database profile configuration"
```

---

### Task 3: Dameng Migration Contract Test

**Files:**
- Create: `server/src/test/java/com/aiworkflow/config/DamengSchemaMigrationContractTest.java`

- [ ] **Step 1: Write the failing contract test**

Create `server/src/test/java/com/aiworkflow/config/DamengSchemaMigrationContractTest.java`:

```java
package com.aiworkflow.config;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aiworkflow.bot.persistence.AiBotEntity;
import com.aiworkflow.bot.persistence.BotMessageEntity;
import com.aiworkflow.bot.persistence.BotSessionEntity;
import com.aiworkflow.knowledge.persistence.KnowledgeBaseEntity;
import com.aiworkflow.knowledge.persistence.KnowledgeChunkEntity;
import com.aiworkflow.knowledge.persistence.KnowledgeChunkVectorEntity;
import com.aiworkflow.knowledge.persistence.KnowledgeDocumentEntity;
import com.aiworkflow.knowledge.persistence.VectorStoreConfigEntity;
import com.aiworkflow.model.persistence.ModelProviderEntity;
import com.aiworkflow.prompt.persistence.PromptTemplateEntity;
import com.aiworkflow.workflow.persistence.WorkflowEntity;
import com.aiworkflow.workflow.persistence.WorkflowExecutionEntity;
import com.aiworkflow.workflow.persistence.WorkflowNodeExecutionEntity;
import com.aiworkflow.workflow.persistence.WorkflowVersionEntity;
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
            WorkflowVersionEntity.class
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
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd server
mvn -Dtest=DamengSchemaMigrationContractTest test
```

Expected: fail because there are no Dameng migration SQL files.

- [ ] **Step 3: Commit the failing contract test only if local workflow allows red commits; otherwise keep it uncommitted**

If committing red tests is allowed:

```bash
git add server/src/test/java/com/aiworkflow/config/DamengSchemaMigrationContractTest.java
git commit -m "test: define dameng schema contract"
```

If red commits are not desired, continue to Task 4 without committing.

---

### Task 4: Dameng Final Schema Migration

**Files:**
- Create: `server/src/main/resources/db/migration/dameng/V1__agi_schema.sql`

- [ ] **Step 1: Add the minimal Dameng schema SQL**

Create `server/src/main/resources/db/migration/dameng/V1__agi_schema.sql` with direct final-table DDL:

```sql
CREATE TABLE agi_workflow (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    status VARCHAR(32) NOT NULL,
    current_version_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE agi_workflow_version (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL,
    definition_json CLOB NOT NULL,
    status VARCHAR(32) NOT NULL,
    published_by VARCHAR(64),
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_agi_workflow_version UNIQUE (workflow_id, version),
    CONSTRAINT fk_agi_workflow_version_workflow FOREIGN KEY (workflow_id) REFERENCES agi_workflow(id)
);

CREATE TABLE agi_workflow_execution (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    workflow_version_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json CLOB NOT NULL,
    context_json CLOB NOT NULL,
    output_json CLOB,
    error_code VARCHAR(100),
    error_message CLOB,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_workflow_execution_workflow FOREIGN KEY (workflow_id) REFERENCES agi_workflow(id),
    CONSTRAINT fk_agi_workflow_execution_version FOREIGN KEY (workflow_version_id) REFERENCES agi_workflow_version(id)
);

CREATE TABLE agi_workflow_node_execution (
    id VARCHAR(64) PRIMARY KEY,
    workflow_execution_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(100) NOT NULL,
    node_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json CLOB,
    output_json CLOB,
    error_code VARCHAR(100),
    error_message CLOB,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_node_execution_run FOREIGN KEY (workflow_execution_id) REFERENCES agi_workflow_execution(id)
);

CREATE TABLE agi_integration_app (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE agi_webhook_subscription (
    id VARCHAR(64) PRIMARY KEY,
    integration_app_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    target_url CLOB NOT NULL,
    secret_ref VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_webhook_app FOREIGN KEY (integration_app_id) REFERENCES agi_integration_app(id)
);

CREATE TABLE agi_prompt_template (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    template CLOB NOT NULL,
    description CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE agi_model_provider (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    base_url CLOB NOT NULL,
    api_key_ref VARCHAR(200) NOT NULL,
    enabled NUMBER(1) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    model_type VARCHAR(100) DEFAULT 'CUSTOM' NOT NULL,
    description CLOB,
    vision_support NUMBER(1) DEFAULT 0 NOT NULL,
    price_per_million_tokens NUMERIC(18, 6) DEFAULT 0 NOT NULL,
    model VARCHAR(200) DEFAULT '' NOT NULL,
    model_usage VARCHAR(40) DEFAULT 'CHAT' NOT NULL
);

CREATE TABLE agi_vector_store_config (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    store_type VARCHAR(64) NOT NULL,
    endpoint CLOB,
    index_name VARCHAR(200) NOT NULL,
    enabled NUMBER(1) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    username VARCHAR(200),
    password CLOB,
    api_key CLOB,
    connect_timeout_ms INTEGER DEFAULT 5000 NOT NULL,
    read_timeout_ms INTEGER DEFAULT 30000 NOT NULL
);

CREATE TABLE agi_knowledge_base (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    embedding_model_id VARCHAR(64),
    vector_store_config_id VARCHAR(64),
    splitter_type VARCHAR(64) NOT NULL,
    chunk_size INTEGER NOT NULL,
    chunk_overlap INTEGER NOT NULL,
    retrieval_mode VARCHAR(64) NOT NULL,
    top_k INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    document_count INTEGER NOT NULL,
    chunk_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    vector_dimension INTEGER DEFAULT 1536 NOT NULL
);

CREATE TABLE agi_knowledge_document (
    id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL,
    name VARCHAR(300) NOT NULL,
    chunk_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_knowledge_document_base FOREIGN KEY (knowledge_base_id) REFERENCES agi_knowledge_base(id) ON DELETE CASCADE
);

CREATE TABLE agi_knowledge_chunk (
    id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    document_name VARCHAR(300) NOT NULL,
    content CLOB NOT NULL,
    chunk_index INTEGER NOT NULL,
    enabled NUMBER(1) NOT NULL,
    token_estimate INTEGER NOT NULL,
    CONSTRAINT fk_agi_knowledge_chunk_base FOREIGN KEY (knowledge_base_id) REFERENCES agi_knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT fk_agi_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES agi_knowledge_document(id) ON DELETE CASCADE
);

CREATE TABLE agi_knowledge_chunk_vector (
    chunk_id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    embedding_model_id VARCHAR(64) NOT NULL,
    embedding CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_chunk_vector_chunk FOREIGN KEY (chunk_id) REFERENCES agi_knowledge_chunk(id) ON DELETE CASCADE,
    CONSTRAINT fk_agi_chunk_vector_base FOREIGN KEY (knowledge_base_id) REFERENCES agi_knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT fk_agi_chunk_vector_document FOREIGN KEY (document_id) REFERENCES agi_knowledge_document(id) ON DELETE CASCADE
);

CREATE TABLE agi_ai_bot (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    avatar VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64),
    model_provider_id VARCHAR(64),
    knowledge_base_id VARCHAR(64),
    system_prompt CLOB,
    opening_message CLOB,
    status VARCHAR(32) NOT NULL,
    conversation_count INTEGER NOT NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE agi_bot_session (
    id VARCHAR(64) PRIMARY KEY,
    bot_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_bot_session_bot FOREIGN KEY (bot_id) REFERENCES agi_ai_bot(id) ON DELETE CASCADE
);

CREATE TABLE agi_bot_message (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    bot_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_bot_message_session FOREIGN KEY (session_id) REFERENCES agi_bot_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_agi_bot_message_bot FOREIGN KEY (bot_id) REFERENCES agi_ai_bot(id) ON DELETE CASCADE
);

ALTER TABLE agi_workflow ADD CONSTRAINT fk_agi_workflow_current_version FOREIGN KEY (current_version_id) REFERENCES agi_workflow_version(id);

CREATE INDEX idx_agi_workflow_tenant_status ON agi_workflow(tenant_id, status);
CREATE INDEX idx_agi_workflow_execution_workflow ON agi_workflow_execution(workflow_id, created_at);
CREATE INDEX idx_agi_node_execution_run ON agi_workflow_node_execution(workflow_execution_id);
CREATE INDEX idx_agi_prompt_template_tenant ON agi_prompt_template(tenant_id, name);
CREATE INDEX idx_agi_model_provider_tenant ON agi_model_provider(tenant_id, enabled);
CREATE INDEX idx_agi_model_provider_usage ON agi_model_provider(tenant_id, model_usage, enabled);
CREATE INDEX idx_agi_knowledge_base_status ON agi_knowledge_base(status, updated_at);
CREATE INDEX idx_agi_knowledge_document_base ON agi_knowledge_document(knowledge_base_id, created_at);
CREATE INDEX idx_agi_knowledge_chunk_document ON agi_knowledge_chunk(knowledge_base_id, document_id, chunk_index);
CREATE INDEX idx_agi_vector_store_enabled ON agi_vector_store_config(enabled, updated_at);
CREATE INDEX idx_agi_chunk_vector_base ON agi_knowledge_chunk_vector(knowledge_base_id, embedding_model_id);
CREATE INDEX idx_agi_chunk_vector_document ON agi_knowledge_chunk_vector(knowledge_base_id, document_id);
CREATE INDEX idx_agi_ai_bot_status ON agi_ai_bot(status, updated_at);
CREATE INDEX idx_agi_ai_bot_workflow ON agi_ai_bot(workflow_id);
CREATE INDEX idx_agi_bot_session_bot ON agi_bot_session(bot_id, updated_at);
CREATE INDEX idx_agi_bot_message_session ON agi_bot_message(bot_id, session_id, created_at);
```

- [ ] **Step 2: Run Dameng contract test**

Run:

```bash
cd server
mvn -Dtest=DamengSchemaMigrationContractTest test
```

Expected: pass.

- [ ] **Step 3: Commit**

```bash
git add server/src/main/resources/db/migration/dameng/V1__agi_schema.sql server/src/test/java/com/aiworkflow/config/DamengSchemaMigrationContractTest.java
git commit -m "feat: add dameng schema migration"
```

---

### Task 5: Full Verification

**Files:**
- No additional files expected.

- [ ] **Step 1: Run focused persistence tests**

Run:

```bash
cd server
mvn -Dtest=FlywaySchemaMigrationTest,DatabaseConfigurationTest,DamengSchemaMigrationContractTest,MybatisPersistenceIntegrationTest test
```

Expected: pass.

- [ ] **Step 2: Run all server tests**

Run:

```bash
cd server
mvn test
```

Expected: pass.

- [ ] **Step 3: Check final git diff**

Run:

```bash
git status --short
git diff --stat
```

Expected: only intended source, resource, and test changes remain. Existing unrelated `server/.idea/` may still be untracked and should not be staged.

- [ ] **Step 4: Commit any remaining verification-only adjustments**

If a small adjustment was required during verification:

```bash
git add server/src/main/resources server/src/test/java/com/aiworkflow/config server/src/test/java/com/aiworkflow/persistence
git commit -m "chore: verify database migration support"
```

If no adjustment was required, do not create an empty commit.

