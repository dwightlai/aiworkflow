# Milvus and Pgvector Vector Store Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add production-capable Milvus and independently deployed Pgvector stores to the existing knowledge ingestion, retrieval, reparse, and deletion flows.

**Architecture:** Replace direct Elasticsearch calls in `KnowledgeBaseService` with a provider registry. Keep local vectors as the source of truth, while Elasticsearch, Milvus, and Pgvector providers implement one external-store contract. Extend the existing vector configuration model and render provider-specific fields in the admin UI.

**Tech Stack:** Java 17, Spring Boot 3.3, MyBatis-Plus, Flyway, Milvus Java SDK, PostgreSQL JDBC, pgvector-java, HikariCP, React, TypeScript, Ant Design, Vitest, JUnit 5.

---

### Task 1: Extend vector store configuration persistence

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/domain/VectorStoreConfig.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/persistence/VectorStoreConfigEntity.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/MybatisVectorStoreConfigStore.java`
- Create: `server/src/main/resources/db/migration/postgresql/V49__vector_store_providers.sql`
- Create: `server/src/main/resources/db/migration/dameng/V10__vector_store_providers.sql`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/VectorStoreConfigPersistenceTest.java`

- [ ] Write a failing mapper/domain round-trip test covering host, port, database, namespace, dimension, SSL, and options.
- [ ] Run the focused test and verify that the new fields do not compile or round-trip.
- [ ] Extend the domain, entity, mapper conversion, and both database migrations.
- [ ] Run the focused test and existing vector config tests.

### Task 2: Introduce the external vector store contract and registry

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/VectorStoreProvider.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/VectorStoreProviderRegistry.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/VectorStoreSearchRequest.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/VectorStoreSearchHit.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/VectorStoreConnectionResult.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/vector/VectorStoreProviderRegistryTest.java`

- [ ] Write failing tests for case-insensitive provider lookup and unsupported-type rejection.
- [ ] Run the tests and verify the registry is missing.
- [ ] Implement the provider contract and registry.
- [ ] Run the registry tests.

### Task 3: Adapt Elasticsearch to the provider contract

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/ElasticsearchVectorStoreProvider.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/ElasticsearchVectorStoreClient.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/vector/ElasticsearchVectorStoreProviderTest.java`

- [ ] Write failing adapter tests for ensure, upsert, search, and delete delegation.
- [ ] Implement the Elasticsearch provider without changing existing ES request semantics.
- [ ] Run existing Elasticsearch client tests and the adapter tests.

### Task 4: Implement Pgvector provider

**Files:**
- Modify: `server/pom.xml`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/PgvectorDataSourceRegistry.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/PgvectorVectorStoreProvider.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/vector/PgvectorVectorStoreProviderTest.java`

- [ ] Write failing tests for identifier validation, JDBC URL construction, initialization SQL, upsert, filtered cosine search, and idempotent deletes.
- [ ] Add pgvector-java and use the existing Hikari dependency supplied by Spring Boot.
- [ ] Implement one cached independent data source per configuration ID.
- [ ] Implement extension verification, table/index creation, dimension verification, upsert, search, and deletion.
- [ ] Run Pgvector provider tests.

### Task 5: Implement Milvus provider

**Files:**
- Modify: `server/pom.xml`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/MilvusClientFactory.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/vector/MilvusVectorStoreProvider.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/vector/MilvusVectorStoreProviderTest.java`

- [ ] Write failing tests around a small client gateway for schema creation, dimension checks, upsert, scalar-filtered search, and deletes.
- [ ] Add the official Milvus Java SDK compatible with Milvus 2.5/2.6 and Java 17.
- [ ] Implement connection construction, collection initialization, COSINE AUTOINDEX, upsert, search, and delete expressions.
- [ ] Escape scalar filter literals and validate collection names.
- [ ] Run Milvus provider tests.

### Task 6: Extend vector configuration service and API

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/VectorStoreConfigService.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/api/VectorStoreConfigController.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/VectorStoreConfigServiceTest.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/api/VectorStoreConfigControllerTest.java`

- [ ] Write failing tests for provider-specific defaults and validation.
- [ ] Write failing tests for unsaved and saved connection-test endpoints.
- [ ] Route enabled configuration initialization through the registry.
- [ ] Preserve existing secrets on edit and hide them in responses.
- [ ] Add `POST /test-connection` and `POST /{id}/test-connection`.
- [ ] Run service and controller tests.

### Task 7: Route knowledge operations through selected provider

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeBaseService.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/KnowledgeBaseVectorStoreRoutingTest.java`

- [ ] Write failing tests proving Milvus selection invokes only Milvus for upsert, search, document delete, knowledge-base delete, and reparse.
- [ ] Add equivalent Pgvector routing tests.
- [ ] Replace direct Elasticsearch dependencies and calls with registry/provider calls.
- [ ] Reject disabled or unsupported configured stores without silent fallback.
- [ ] Validate knowledge-base embedding dimension against configuration dimension.
- [ ] Run knowledge service tests.

### Task 8: Add admin API types and provider-specific form

**Files:**
- Modify: `web/apps/admin/src/api/knowledge.ts`
- Modify: `web/apps/admin/src/pages/knowledge/KnowledgeBasesPage.tsx`
- Test: `web/apps/admin/src/api/knowledge.test.ts`
- Test: `web/apps/admin/src/pages/knowledge/KnowledgeBasesPage.test.tsx`

- [ ] Write failing tests for Milvus/Pgvector options, dynamic fields, defaults, payloads, and connection testing.
- [ ] Extend API types and add connection-test calls.
- [ ] Render dynamic Milvus and Pgvector fields with Ant Design controls.
- [ ] Add a test-connection command and clear success/failure feedback.
- [ ] Run focused frontend tests and the admin build.

### Task 9: Full verification and service restart

**Files:**
- Modify only if verification exposes a defect.

- [ ] Run all vector configuration, provider, knowledge routing, and API tests.
- [ ] Run `mvn -q -DskipTests package`.
- [ ] Run frontend tests and `pnpm --filter @aiworkflow/admin build`.
- [ ] Restart 8080 while retaining 5173 and 5174.
- [ ] Verify Flyway migration 49, API startup, and listeners.
- [ ] Exercise configuration API validation for Milvus and Pgvector without requiring a live external database.
