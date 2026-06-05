# Knowledge Add Data Design

## Scope

This change improves the knowledge base document ingestion experience and backend ingestion model. In the knowledge base management page, users add data under the selected knowledge base through one "Add Data" button with a dropdown menu.

Supported dataset types:

- `MANUAL`: manual dataset entries.
- `TEXT_DOCUMENT`: uploaded text documents.
- `TABLE_DOCUMENT`: uploaded table documents.

The existing embedding and Elasticsearch write path remains the shared final processing path after content is parsed and split.

## Entry And Navigation

In the selected knowledge base document drawer, replace the mixed manual and upload panels with a focused action:

- Primary button: "Add Data" in Chinese UI text.
- Dropdown items:
  - Manual Dataset
  - Text Document
  - Table Document

Selecting an item opens the corresponding drawer or modal. Each mode keeps its own form state and validation.

## Manual Dataset

Manual dataset creation supports adding one or more entries in one submit.

Each entry contains:

- Title
- Body
- Tags
- Category
- Source note

On submit, backend converts each entry into searchable text with stable labels:

```text
Title: ...
Category: ...
Tags: ...
Source: ...
Body:
...
```

Manual entries use the knowledge base default ingestion behavior. They are split, embedded, stored locally, and written to the configured Elasticsearch vector index when the knowledge base has an external vector store.

## Text Documents

Supported text document extensions:

- `txt`
- `md`
- `doc`
- `docx`
- `pdf`
- `html`

The text document upload UI has a dedicated splitting configuration panel. It does not show table splitting options and does not include the large-model semantic splitter.

Text splitter options are mutually exclusive radio options:

- `FIXED_LENGTH`: fixed length splitting. Default selected.
- `PARAGRAPH`: paragraph splitting.
- `SEMANTIC`: local semantic splitting.
- `SYMBOL`: custom symbol splitting.

Common text splitter behavior:

- Default split length is `200`.
- Split length is required and must be at least `1`.
- The control supports direct numeric input and minus/plus adjustment.
- The selected file list is shown beside the processing panel.
- Preview and final upload use the currently selected splitter options.

Fixed length:

- Only shows split length.
- Splits text by exact character length.
- The final remainder becomes its own chunk.

Paragraph:

- Only shows split length.
- Splits first by native newlines and blank paragraph boundaries.
- Paragraphs longer than the configured length are split by fixed length.
- Paragraphs shorter than the configured length are preserved whole.

Local semantic:

- Only shows split length.
- Uses local sentence and paragraph boundaries, punctuation, and simple text cohesion heuristics.
- Chunks should not exceed the configured length.
- It does not call a large model in this version.

Symbol:

- Shows split length and split symbol.
- The split symbol input placeholder is "Please enter split symbol" in Chinese UI text.
- Splits first by the user-provided symbol.
- Segments longer than the configured length are split by fixed length.

## Table Documents

Supported table document extensions:

- `xls`
- `xlsx`

The table upload UI has a dedicated structured splitting panel. It does not show text splitter options.

Table splitter options:

- `STRUCTURED_TABLE`: the only table splitter option.
- Split length default is `200`.
- Split length is required and must be at least `1`.
- The control supports direct numeric input and minus/plus adjustment.

Table parsing rules:

- Base unit is a table row.
- Empty rows and rows where all cells are empty are ignored.
- Multiple sheets are processed separately. Rows are aggregated only within the same sheet.
- The first non-empty row is treated as the header row when possible.
- Each data row is converted into text using field names and field values:

```text
Sheet: Sheet1
Field A: XX; Field B: XX
```

- Consecutive rows are aggregated into one chunk while the accumulated text does not exceed split length.
- If adding the next row would exceed the configured length, the current accumulated content becomes a chunk and the next row starts a new chunk.
- If a single row text exceeds split length, it is split by fixed character length.

## Document List And Operations

The document list shows:

- Name
- Dataset type
- Processing status
- Tags
- Category
- Source
- Row count
- Chunk count
- Created time

Operations:

- View chunks
- Enable or disable chunks
- Delete
- Reparse

## Reparse

Reparse is fully implemented in this version.

Backend behavior:

- Reparse uses the saved dataset type and splitter configuration.
- For manual datasets, saved manual entries are converted again to searchable text.
- For text and table documents, the saved original raw text or parsed source payload is used for re-splitting.
- Existing chunks, local vectors, and external Elasticsearch documents for the document are deleted before new chunks are saved.
- New chunks are embedded and written to the configured vector store.
- Knowledge base document and chunk statistics are refreshed after reparse.

Status handling:

- Set status to `PROCESSING` during reparse.
- Set status to `READY` after successful completion.
- Set status to `FAILED` and store an error message when parsing, splitting, embedding, or indexing fails.

Frontend behavior:

- Reparse is available in the document list.
- It is disabled while the document is processing.
- After completion, the document list, knowledge base stats, and chunk list are refreshed.

## Backend Model Changes

`KnowledgeDocument` gains:

- `datasetType`
- `processingStatus`
- `tags`
- `category`
- `source`
- `rowCount`
- `parserType`
- `splitterType`
- `splitterConfig`
- `rawContent`
- `errorMessage`

Migration requirements:

- Add the fields to PostgreSQL migration.
- Add matching fields to Dameng migration and contract tests.
- Preserve existing rows by defaulting `datasetType` to `TEXT_DOCUMENT` and `processingStatus` to `READY`.

## API Changes

New or updated endpoints:

- `POST /api/knowledge-bases/{id}/documents/manual`
- `POST /api/knowledge-bases/{id}/documents/text/upload`
- `POST /api/knowledge-bases/{id}/documents/table/upload`
- `POST /api/knowledge-bases/documents/text/upload/preview`
- `POST /api/knowledge-bases/documents/table/upload/preview`
- `POST /api/knowledge-bases/{id}/documents/{documentId}/reparse`

Existing document list, chunk list, chunk update, and delete endpoints remain.

## Testing

Backend tests cover:

- Manual dataset creates multiple entries and produces chunks.
- Text fixed length, paragraph, semantic, and symbol splitters.
- Table structured splitting, empty-row filtering, multi-sheet isolation, row aggregation, and single-row overflow fallback.
- Reparse deletes old chunks, vectors, and external documents, then writes new chunks and vectors.
- Document metadata persistence through in-memory and MyBatis stores.

Frontend tests cover:

- The Add Data dropdown opens the correct mode.
- Text document splitter options dynamically show the expected fields.
- Table document upload only shows structured splitting.
- Manual dataset supports multiple entries.
- Reparse action calls the API and refreshes document and chunk data.
