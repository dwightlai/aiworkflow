# Standardized Excel Chunking Design

## Goal

Improve knowledge-base chunking for normalized Excel records without making the
knowledge-base module responsible for complex workbook recognition, cleanup, or
business relationship inference.

## Responsibility Boundary

The upstream document parsing and cleanup flow is responsible for:

- detecting tables and data regions;
- flattening multi-row headers;
- expanding merged cells where required;
- separating sub-tables;
- removing titles, totals, signatures, and other noise;
- producing one normalized schema and a sequence of independent records.

The knowledge-base module assumes that input is already normalized. It does not
infer relationships between workbooks, worksheets, adjacent rows, or similar
identifier prefixes.

## Chunk Model

Each normalized record produces one `TABLE_ROW` child chunk.

- `content`: readable key-value lines containing only non-empty fields.
- `embeddingContent`: a concise natural-language record containing field names
  and values, without a standalone schema row or Markdown separators.
- `metadataJson`: structured source and record data.
- `atomic`: `true`; records are never merged with adjacent rows.
- `groupId`: identifies the source table only, not a business relationship.

Required metadata:

```json
{
  "sourceFile": "XLSX_0301_采购台账类.xlsx",
  "sheetName": "采购台账",
  "rowIndex": 2,
  "schema": ["采购编号", "申请部门", "预算金额"],
  "data": {
    "采购编号": "CG-2026-0301-01",
    "申请部门": "交付部",
    "预算金额": 840000
  },
  "fieldTypes": {
    "采购编号": "STRING",
    "申请部门": "STRING",
    "预算金额": "NUMBER"
  }
}
```

Blank fields remain represented in `data` for structural filtering but are
omitted from `content` and `embeddingContent`.

## Data Flow

1. `DocumentTextExtractor` reads normalized XLSX rows and emits the existing
   Markdown transport representation.
2. `OfficeStructureParser` converts each table row into a structured
   `DocumentNode`, parsing header/value pairs and attaching source metadata.
3. `DefaultNodeChunkStrategy` serializes node metadata into
   `KnowledgeChunkPreview.metadataJson` and generates table-specific embedding
   text.
4. Preview, upload, vectorization, and reparse continue through the same
   `splitDocument` entry point.
5. Persistence keeps structured fields in the existing `metadata_json` column;
   no schema migration is required.

## Length Policy

The configured chunk length is a maximum-size safeguard for one record. It does
not combine multiple rows. Standard records remain atomic. Oversized normalized
records are a later internal parent-child enhancement and must never pull in an
adjacent row.

## Acceptance Criteria

- Every normalized source row produces exactly one `TABLE_ROW`.
- Preview content contains key-value lines and no Markdown schema/separator row.
- Metadata includes source file, sheet, source row, schema, data, and field types.
- Embedding text has no standalone schema list and excludes empty fields.
- No row relationship is inferred.
- Preview, formal upload, and reparse produce equivalent chunks.
- Existing DOCX, PDF, real-format, and 500-document regressions remain green.
