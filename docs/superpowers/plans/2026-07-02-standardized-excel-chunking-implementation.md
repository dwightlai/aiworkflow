# Standardized Excel Chunking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce clean, structured, independently retrievable chunks for normalized Excel rows.

**Architecture:** Keep the existing XLSX-to-Markdown transport, enrich `TABLE_ROW`
nodes in `OfficeStructureParser`, and let `DefaultNodeChunkStrategy` create
separate display, embedding, and metadata representations. Persist through the
existing `metadata_json` channel.

**Tech Stack:** Java 17, Spring Boot, Apache POI, Jackson, JUnit 5, AssertJ

---

### Task 1: Define Structured Row Behavior

**Files:**
- Modify: `server/src/test/java/com/mw/ai/agi/knowledge/chunking/OfficeStructureParserTest.java`
- Modify: `server/src/test/java/com/mw/ai/agi/knowledge/service/ExcelTableUploadChunkingTest.java`

- [ ] Add failing tests asserting parsed field maps, source row numbers, schema,
  inferred field types, clean display content, and clean embedding content.
- [ ] Run `mvn -q -Dtest=OfficeStructureParserTest,ExcelTableUploadChunkingTest test`.
- [ ] Confirm failures are caused by missing structured metadata.

### Task 2: Parse Normalized Rows

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/OfficeStructureParser.java`

- [ ] Parse escaped Markdown cells without losing empty values.
- [ ] Build ordered `schema`, `data`, and `fieldTypes` metadata.
- [ ] Produce key-value node text and preserve source sheet and row.
- [ ] Re-run the focused parser tests.

### Task 3: Generate Preview and Embedding Representations

**Files:**
- Modify: `server/src/main/java/com/mw/ai/agi/knowledge/chunking/DefaultNodeChunkStrategy.java`

- [ ] Serialize document and node metadata into preview `metadataJson`.
- [ ] Generate concise table-record embedding text from non-empty `data`.
- [ ] Keep generic document chunk behavior unchanged.
- [ ] Re-run focused preview tests.

### Task 4: Regression and Runtime Verification

**Files:**
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/Business500DatasetRegressionTest.java`
- Test: `server/src/test/java/com/mw/ai/agi/knowledge/service/RealFormatChunkingRegressionTest.java`

- [ ] Run focused Excel tests.
- [ ] Run office parser, real-format, vector-search, and 500-document regressions.
- [ ] Restart the backend on port `8080`.
- [ ] Verify ports `5173`, `5174`, and `8080` are listening.
