# Knowledge Chunking Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved V1.3.1 knowledge chunking design from unified structure-aware splitting through parent/child retrieval and real DOCX/XLSX/PDF parsing.

**Architecture:** Existing controllers and API contracts remain compatible while `KnowledgeDocumentSplitter` becomes the single facade. Format parsers produce `DocumentStructure`; node strategies produce enriched previews; persistence stores parent/group/version metadata; retrieval expands authorized evidence in batches.

**Tech Stack:** Java 17, Spring Boot 3.3, Apache POI 5.2, Apache Tika 2.9, MyBatis-Plus, PostgreSQL, Dameng, Elasticsearch, JUnit 5, AssertJ.

---

### Task 1: Unified chunking model and token counter

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/DocumentNode.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/DocumentStructure.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/ChunkProfile.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/TokenCounter.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/HeuristicTokenCounter.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/domain/KnowledgeChunkPreview.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeSplitRequest.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/chunking/HeuristicTokenCounterTest.java`

- [ ] Write failing tests for Chinese, English, code-like text, overlap, and backward-compatible constructors.
- [ ] Run the focused tests and confirm expected failures.
- [ ] Add the model records, token counter, and compatibility constructors.
- [ ] Re-run the focused tests and existing splitter tests.

### Task 2: Markdown/TXT/CSV structure parsing

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/StructuredDocumentParser.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/MarkdownStructureParser.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/PlainTextStructureParser.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/CsvStructureParser.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/chunking/MarkdownStructureParserTest.java`

- [ ] Write failing tests for headings, same-name headings, fenced code, FAQ, lists, table rows, meeting topics, and section paths.
- [ ] Verify the tests fail because parsers are absent.
- [ ] Implement parsers producing ordered typed nodes and metadata.
- [ ] Verify parser tests pass.

### Task 3: Node-level strategy router and unified splitter

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/NodeChunkStrategy.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/ChunkStrategyRouter.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/DefaultChunkStrategyRouter.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeDocumentSplitter.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeSplitter.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/KnowledgeDocumentSplitterTest.java`

- [ ] Add failing tests for `STRUCTURE_AWARE`, `FAQ_PAIR`, `TABLE_ROW_GROUP`, `ATOMIC_CODE`, `PROCEDURE_STEP`, `CLAUSE_GROUP`, `MEETING_TOPIC`, `SENTENCE_BOUNDARY`, and overlap.
- [ ] Verify failures.
- [ ] Implement node routing and make `KnowledgeSplitter` a compatibility delegate.
- [ ] Verify preview, upload, and reparse use the same splitter results.

### Task 4: Enriched chunk persistence and migrations

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/domain/KnowledgeChunk.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/persistence/KnowledgeChunkEntity.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/MybatisKnowledgeStore.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeBaseService.java`
- Create: `server/src/main/resources/db/migration/postgresql/V46__knowledge_chunking_v2.sql`
- Create: `server/src/main/resources/db/migration/dameng/V9__knowledge_chunking_v2.sql`
- Test: `server/src/test/java/com/mw/ai/agi/persistence/MybatisPersistenceIntegrationTest.java`

- [ ] Add failing persistence tests for logical ID, parent ID, group ID, level, section path, and embedding snapshot metadata.
- [ ] Verify failures.
- [ ] Add compatible fields and migrations.
- [ ] Persist parent/child chunks while embedding only searchable child chunks.
- [ ] Verify H2/MyBatis tests and migration syntax.

### Task 5: Authorized evidence expansion

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/domain/EvidenceGroup.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeContextAssembler.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeBaseService.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeRetrievalFilters.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/KnowledgeContextAssemblerTest.java`

- [ ] Add failing tests proving unauthorized parents/neighbors never expand.
- [ ] Add tests for parent, group, neighbor, reference expansion, deduplication, and token limits.
- [ ] Implement batch assembly and flat-result compatibility projection.
- [ ] Verify security tests report zero unauthorized evidence.

### Task 6: DOCX, XLSX, and PDF structured parsers

**Files:**
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/DocxStructureParser.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/XlsxStructureParser.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/PdfStructureParser.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/DocumentTextExtractor.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/chunking/RealFormatStructureParserTest.java`

- [ ] Add failing tests using files under `sampledata/chunkSample/rag_chunking_real_format_samples`.
- [ ] Implement DOCX headings, tables, notes/captions, meeting topics, and archive hierarchy.
- [ ] Implement XLSX sheets, headers, rows, and hierarchical records.
- [ ] Implement PDF pages, repeated header/footer suppression, reading order metadata, and captions using available Tika/PDF parser data.
- [ ] Verify the 11 files parse into non-empty typed structures.

### Task 7: Regression harness and quality reporting

**Files:**
- Create: `server/src/test/java/com/mw/ai/agi/knowledge/chunking/ChunkingSampleRegressionTest.java`
- Create: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/ChunkingQualityReport.java`
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeBaseService.java`

- [ ] Load the 14 algorithm questions and 13 main business questions.
- [ ] Assert structural completeness and expected-evidence coverage.
- [ ] Record chunk counts, token distribution, atomic blocks, expansions, trims, and permission filters.
- [ ] Verify P0/P1 metrics and produce a baseline report for real-format P2 metrics.

### Task 8: Admin API and preview compatibility

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/api/KnowledgeBaseController.java`
- Modify: `web/apps/admin/src/api/knowledge.ts`
- Modify: `web/apps/admin/src/pages/knowledge/KnowledgeBasesPage.tsx`
- Test: `web/apps/admin/src/pages/knowledge/KnowledgeBasesPage.test.tsx`

- [ ] Add failing API/UI tests for chunk type, section path, parent/child level, atomic flag, and split reason.
- [ ] Extend responses without removing old fields.
- [ ] Show enriched metadata in the existing preview/editor.
- [ ] Run backend and frontend test suites.

### Task 9: Full verification

- [ ] Run `mvn test` in `server`.
- [ ] Run admin frontend tests and build.
- [ ] Run the 14-question algorithm regression.
- [ ] Run the 13-question main business regression.
- [ ] Confirm Java 17 compilation, PostgreSQL migration ordering, and Dameng migration parity.
- [ ] Document any parser limitations that cannot be solved without an external OCR/layout engine.
