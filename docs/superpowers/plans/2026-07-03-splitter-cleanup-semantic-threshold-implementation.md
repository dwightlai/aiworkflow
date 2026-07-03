# Splitter Cleanup and Semantic Threshold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `KnowledgeSplitter` completely and provide a persisted, configurable semantic similarity threshold from the admin UI through document chunking.

**Architecture:** `HeuristicTokenCounter` becomes the single token estimator used by `KnowledgeBaseService` and `KnowledgeDocumentSplitter`. The semantic threshold is stored on `KnowledgeBase`, copied into each effective `KnowledgeSplitRequest`, persisted in document splitter JSON, and exposed only for semantic splitting in the admin forms.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Flyway, PostgreSQL/Dameng, React, TypeScript, Ant Design, Vitest, JUnit 5.

---

### Task 1: Remove KnowledgeSplitter compatibility

**Files:**
- Delete: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeSplitter.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeBaseService.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeDocumentSplitter.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/KnowledgeDocumentSplitterTest.java`

- [ ] Add a source contract test asserting production Java sources contain no `KnowledgeSplitter` reference.
- [ ] Run the contract test and confirm it fails.
- [ ] Replace service token estimation with injected `TokenCounter`.
- [ ] Replace compatibility constructors with explicit test factories using `HeuristicTokenCounter`, parsers, router, and embedding client.
- [ ] Delete `KnowledgeSplitter.java` and update all tests.
- [ ] Run knowledge service tests and confirm they pass.

### Task 2: Persist semantic threshold

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/domain/KnowledgeBase.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/persistence/KnowledgeBaseEntity.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/MybatisKnowledgeStore.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/api/KnowledgeBaseController.java`
- Create: PostgreSQL and Dameng Flyway migrations.
- Test: knowledge store and API tests.

- [ ] Add failing tests for default `0.78`, clamping to `0-1`, and persistence mapping.
- [ ] Add `semanticSimilarityThreshold` to domain/entity/API contracts.
- [ ] Add database migrations with default `0.78`.
- [ ] Run persistence and API tests.

### Task 3: Pass threshold through every split path

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeBaseService.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/ChunkProfileService.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeSplitRequest.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/api/KnowledgeBaseController.java`

- [ ] Add failing tests proving knowledge-base defaults and upload overrides reach the semantic chunker.
- [ ] Extend preview, upload, manual document and reprocess methods with the optional threshold.
- [ ] Persist the effective value in `splitterConfig`.
- [ ] Read `semanticSimilarityThreshold` from Profile `configJson`.
- [ ] Run focused semantic and controller tests.

### Task 4: Add admin UI controls

**Files:**
- Modify: `web/apps/admin/src/api/knowledge.ts`
- Modify: `web/apps/admin/src/pages/knowledge/KnowledgeBasesPage.tsx`
- Modify: `web/apps/admin/src/pages/knowledge/KnowledgeFileUploadWizard.tsx`
- Modify: `web/apps/admin/src/pages/knowledge/ChunkProfilePanel.tsx`
- Test: corresponding API and page tests.

- [ ] Add failing tests for conditional threshold display and multipart/JSON serialization.
- [ ] Add `semanticSimilarityThreshold?: number` to API types.
- [ ] Render `InputNumber(min=0,max=1,step=0.01)` only when strategy is `SEMANTIC`.
- [ ] Initialize from knowledge-base default and allow upload override.
- [ ] Run Vitest and TypeScript build.

### Task 5: Regression and runtime verification

- [ ] Run focused JUnit tests.
- [ ] Run the 500-document regression and confirm 100% parse and at least 90% recall.
- [ ] Run `mvn -q -DskipTests package`.
- [ ] Run `pnpm --filter @aiworkflow/admin build`.
- [ ] Restart port 8080 and verify ports 5173, 5174, and 8080.
