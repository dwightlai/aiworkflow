# Knowledge Add Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the new knowledge-base "Add Data" workflow for manual datasets, text documents, table documents, and full reparse.

**Architecture:** Extend `KnowledgeDocument` metadata and persistence first, then introduce dedicated splitters/parsers that feed the existing chunk, embedding, and Elasticsearch write path. The frontend keeps the current knowledge drawer but replaces mixed panels with an Add Data dropdown and type-specific drawers.

**Tech Stack:** Spring Boot, MyBatis Plus, Flyway PostgreSQL/Dameng migrations, Apache Tika, Ant Design React, Vitest, JUnit 5.

---

### Task 1: Extend Knowledge Document Metadata

**Files:**
- Modify: `server/src/main/java/com/aiworkflow/knowledge/domain/KnowledgeDocument.java`
- Modify: `server/src/main/java/com/aiworkflow/knowledge/persistence/KnowledgeDocumentEntity.java`
- Modify: `server/src/main/java/com/aiworkflow/knowledge/service/InMemoryKnowledgeStore.java`
- Modify: `server/src/main/java/com/aiworkflow/knowledge/service/MybatisKnowledgeStore.java`
- Create: `server/src/main/resources/db/migration/postgresql/V13__knowledge_document_dataset_metadata.sql`
- Modify: `server/src/main/resources/db/migration/dameng/V1__agi_schema.sql`
- Modify tests: `server/src/test/java/com/aiworkflow/persistence/MybatisPersistenceIntegrationTest.java`, `server/src/test/java/com/aiworkflow/config/DamengSchemaMigrationContractTest.java`

- [ ] **Step 1: Write persistence tests for new document fields**

Add assertions that a saved `KnowledgeDocument` preserves dataset metadata:

```java
KnowledgeDocument document = new KnowledgeDocument(
        "doc_1",
        "kb_1",
        "manual faq",
        2,
        Instant.now(),
        "MANUAL",
        "READY",
        "policy,faq",
        "service",
        "manual input",
        2,
        "MANUAL",
        "FIXED_LENGTH",
        "{\"chunkSize\":200}",
        "[{\"title\":\"Refund\",\"content\":\"Refund rule\"}]",
        null
);
knowledgeStore.saveDocument(document);
assertThat(knowledgeStore.listDocuments("kb_1").get(0))
        .extracting(KnowledgeDocument::datasetType, KnowledgeDocument::processingStatus, KnowledgeDocument::tags)
        .containsExactly("MANUAL", "READY", "policy,faq");
```

- [ ] **Step 2: Run the focused persistence test and confirm failure**

Run: `mvn "-Dtest=MybatisPersistenceIntegrationTest,DamengSchemaMigrationContractTest" test`

Expected: compilation failure because `KnowledgeDocument` does not yet expose the new fields.

- [ ] **Step 3: Extend document domain and entity**

Update `KnowledgeDocument` record to include:

```java
String datasetType,
String processingStatus,
String tags,
String category,
String source,
int rowCount,
String parserType,
String splitterType,
String splitterConfig,
String rawContent,
String errorMessage
```

Add matching properties, getters, and setters to `KnowledgeDocumentEntity`.

- [ ] **Step 4: Update store mappings**

Map all new fields in `MybatisKnowledgeStore.toEntity` and `toDomain`. For old in-memory tests, use a static factory or update constructor calls to pass defaults:

```java
"TEXT_DOCUMENT", "READY", null, null, null, 0, "TEXT", "FIXED_LENGTH", "{}", null, null
```

- [ ] **Step 5: Add migrations**

PostgreSQL migration:

```sql
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS dataset_type VARCHAR(32) NOT NULL DEFAULT 'TEXT_DOCUMENT';
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS processing_status VARCHAR(32) NOT NULL DEFAULT 'READY';
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS tags TEXT;
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS category VARCHAR(255);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS source TEXT;
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS row_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS parser_type VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS splitter_type VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS splitter_config TEXT;
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS raw_content TEXT;
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS error_message TEXT;
```

Dameng schema uses the same columns with `VARCHAR2`, `CLOB`, and `NUMBER(10)` as appropriate.

- [ ] **Step 6: Run metadata persistence tests**

Run: `mvn "-Dtest=MybatisPersistenceIntegrationTest,DamengSchemaMigrationContractTest" test`

Expected: PASS.

### Task 2: Add Text And Table Splitter Services

**Files:**
- Create: `server/src/main/java/com/aiworkflow/knowledge/service/KnowledgeSplitRequest.java`
- Create: `server/src/main/java/com/aiworkflow/knowledge/service/KnowledgeDocumentSplitter.java`
- Create: `server/src/main/java/com/aiworkflow/knowledge/service/TableDocumentParser.java`
- Test: `server/src/test/java/com/aiworkflow/knowledge/service/KnowledgeDocumentSplitterTest.java`
- Test: `server/src/test/java/com/aiworkflow/knowledge/service/TableDocumentParserTest.java`

- [ ] **Step 1: Write text splitter tests**

Cover:

```java
assertThat(splitter.splitText("abcdef", fixed(2))).extracting(KnowledgeChunkPreview::content)
        .containsExactly("ab", "cd", "ef");
assertThat(splitter.splitText("para one\n\nparagraph two is long", paragraph(10))).allSatisfy(chunk ->
        assertThat(chunk.content()).hasSizeLessThanOrEqualTo(10));
assertThat(splitter.splitText("A sentence. Another sentence.", semantic(20))).extracting(KnowledgeChunkPreview::content)
        .contains("A sentence.");
assertThat(splitter.splitText("a|bcd|ef", symbol(3, "|"))).extracting(KnowledgeChunkPreview::content)
        .containsExactly("a", "bcd", "ef");
```

- [ ] **Step 2: Write table splitter tests**

Use workbook rows represented by parser output:

```java
List<TableDocumentParser.TableRow> rows = List.of(
        new TableDocumentParser.TableRow("Sheet1", Map.of("Field A", "A1", "Field B", "B1")),
        new TableDocumentParser.TableRow("Sheet1", Map.of("Field A", "A2", "Field B", "B2")),
        new TableDocumentParser.TableRow("Sheet2", Map.of("Field A", "A3"))
);
assertThat(splitter.splitTableRows(rows, structuredTable(200))).hasSize(2);
```

- [ ] **Step 3: Run splitter tests and confirm failure**

Run: `mvn "-Dtest=KnowledgeDocumentSplitterTest,TableDocumentParserTest" test`

Expected: compilation failure because services do not exist.

- [ ] **Step 4: Implement `KnowledgeSplitRequest`**

Use a record:

```java
public record KnowledgeSplitRequest(
        String splitterType,
        int chunkSize,
        String separator
) {
    public int effectiveChunkSize() {
        return chunkSize <= 0 ? 200 : chunkSize;
    }
}
```

- [ ] **Step 5: Implement `KnowledgeDocumentSplitter`**

Implement methods:

```java
List<KnowledgeChunkPreview> splitText(String content, KnowledgeSplitRequest request)
List<KnowledgeChunkPreview> splitTableRows(List<TableDocumentParser.TableRow> rows, KnowledgeSplitRequest request)
```

Supported splitter constants: `FIXED_LENGTH`, `PARAGRAPH`, `SEMANTIC`, `SYMBOL`, `STRUCTURED_TABLE`.

- [ ] **Step 6: Implement `TableDocumentParser`**

Use Apache Tika text extraction as the first implementation. Convert tabular text into rows when Tika preserves tabs/newlines; keep parser isolated so a richer POI parser can replace it later without touching ingestion.

- [ ] **Step 7: Run splitter tests**

Run: `mvn "-Dtest=KnowledgeDocumentSplitterTest,TableDocumentParserTest" test`

Expected: PASS.

### Task 3: Add Manual/Text/Table Ingestion And Reparse APIs

**Files:**
- Modify: `server/src/main/java/com/aiworkflow/knowledge/service/KnowledgeBaseService.java`
- Modify: `server/src/main/java/com/aiworkflow/knowledge/api/KnowledgeBaseController.java`
- Modify tests: `server/src/test/java/com/aiworkflow/knowledge/service/KnowledgeVectorSearchTest.java`
- Modify tests: `server/src/test/java/com/aiworkflow/knowledge/api/KnowledgeBaseControllerIntegrationTest.java`

- [ ] **Step 1: Write service tests**

Add tests for:

```java
service.addManualDataset(kb.id(), List.of(new ManualEntry("Title", "Body", "tag", "cat", "source")));
service.addTextDocumentFile(kb.id(), "faq.txt", "text/plain", input, fixed(200));
service.reparseDocument(kb.id(), document.id());
```

Assert chunks, document metadata, vectors, and reparse replacement behavior.

- [ ] **Step 2: Run tests and confirm failure**

Run: `mvn "-Dtest=KnowledgeVectorSearchTest,KnowledgeBaseControllerIntegrationTest" test`

Expected: compilation failure for missing service/controller methods.

- [ ] **Step 3: Add service records**

Inside `KnowledgeBaseService` or nearby focused files:

```java
public record ManualDatasetEntry(String title, String content, String tags, String category, String source) {}
public record UploadedDocumentPreview(String fileName, int characterCount, List<KnowledgeChunkPreview> chunks) {}
```

Keep existing `UploadedDocumentPreview` compatible.

- [ ] **Step 4: Implement ingestion methods**

Add methods:

```java
public List<KnowledgeDocument> addManualDataset(String knowledgeBaseId, List<ManualDatasetEntry> entries)
public KnowledgeDocument addTextDocumentFile(String knowledgeBaseId, String fileName, String contentType, InputStream inputStream, KnowledgeSplitRequest splitRequest)
public KnowledgeDocument addTableDocumentFile(String knowledgeBaseId, String fileName, String contentType, InputStream inputStream, KnowledgeSplitRequest splitRequest)
public KnowledgeDocument reparseDocument(String knowledgeBaseId, String documentId)
```

All methods save document metadata, create chunks using `KnowledgeDocumentSplitter`, call existing embedding/external ES write behavior, and refresh stats.

- [ ] **Step 5: Implement reparse deletion and status**

Before rebuilding chunks:

```java
externalVectorStore(knowledgeBase).ifPresent(config ->
        elasticsearchVectorStoreClient.deleteDocument(config, knowledgeBaseId, documentId));
store.deleteChunkVectors(knowledgeBaseId, documentId);
store.deleteChunks(knowledgeBaseId, documentId);
```

Set document `PROCESSING`, then `READY` or `FAILED`.

- [ ] **Step 6: Add controller endpoints**

Add:

```java
@PostMapping("/{id}/documents/manual")
@PostMapping("/{id}/documents/text/upload")
@PostMapping("/{id}/documents/table/upload")
@PostMapping("/documents/text/upload/preview")
@PostMapping("/documents/table/upload/preview")
@PostMapping("/{id}/documents/{documentId}/reparse")
```

Keep legacy endpoints working by delegating to fixed-length text ingestion.

- [ ] **Step 7: Run backend API tests**

Run: `mvn "-Dtest=KnowledgeVectorSearchTest,KnowledgeBaseControllerIntegrationTest" test`

Expected: PASS.

### Task 4: Update Frontend API And UI

**Files:**
- Modify: `web/apps/admin/src/api/knowledge.ts`
- Modify: `web/apps/admin/src/pages/knowledge/KnowledgeBasesPage.tsx`
- Modify tests: `web/apps/admin/src/pages/knowledge/KnowledgeBasesPage.test.tsx`

- [ ] **Step 1: Write frontend tests**

Test:

```ts
await userEvent.click(screen.getByRole('button', { name: /新增数据/ }));
await userEvent.click(screen.getByText('文本文档'));
expect(screen.getByLabelText('固定长度分段')).toBeChecked();
await userEvent.click(screen.getByLabelText('符号分段'));
expect(screen.getByPlaceholderText('请输入分段符号')).toBeInTheDocument();
```

Also test table mode only shows structured split and reparse calls API.

- [ ] **Step 2: Run frontend test and confirm failure**

Run: `npm test -- KnowledgeBasesPage.test.tsx --runInBand` from `web/apps/admin` if supported by the repo scripts, otherwise use the existing project test command.

Expected: FAIL because UI/API functions do not exist.

- [ ] **Step 3: Extend API types**

Add document metadata fields, split request types, manual dataset request, text/table upload options, and `reparseKnowledgeDocument`.

- [ ] **Step 4: Replace mixed panels with Add Data dropdown**

Use Ant Design `Dropdown` with menu items:

- `手动数据集`
- `文本文档`
- `表格文档`

Open type-specific drawers.

- [ ] **Step 5: Implement manual dataset drawer**

Use `Form.List` for multiple entries. Fields: title, content, tags, category, source.

- [ ] **Step 6: Implement text document drawer**

Show radio group: fixed length, paragraph, semantic, symbol. Show chunk size stepper for all modes and separator input only for symbol mode.

- [ ] **Step 7: Implement table document drawer**

Show only structured table option and chunk size stepper. Accept only `.xls,.xlsx`.

- [ ] **Step 8: Update document list**

Show dataset type, status, tags, category, source, row count, chunk count, and actions for chunks, reparse, delete.

- [ ] **Step 9: Run frontend tests**

Run the repo's focused knowledge page tests.

Expected: PASS for updated tests.

### Task 5: Full Verification

**Files:**
- No new files.

- [ ] **Step 1: Run backend focused tests**

Run:

```powershell
mvn "-Dtest=KnowledgeVectorSearchTest,KnowledgeBaseControllerIntegrationTest,MybatisPersistenceIntegrationTest,DamengSchemaMigrationContractTest,KnowledgeDocumentSplitterTest,TableDocumentParserTest" test
```

Expected: PASS.

- [ ] **Step 2: Run frontend focused tests**

Run the existing admin frontend test command for knowledge pages.

Expected: PASS or document any pre-existing unrelated timeout.

- [ ] **Step 3: Restart backend**

Stop existing `mvn spring-boot:run` and `com.aiworkflow.AiWorkflowApplication` Java processes for this repo, then start:

```powershell
Start-Process -FilePath mvn -ArgumentList 'spring-boot:run' -WorkingDirectory 'D:\openworkspace\aiworkflow\server' -WindowStyle Hidden
```

- [ ] **Step 4: Smoke test API**

Use Swagger or `Invoke-RestMethod` to create one manual entry, upload one text file with symbol split, upload one table file, and reparse one document.

Expected: document list shows correct types/status and chunks are present.
