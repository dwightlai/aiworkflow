ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS kb_type VARCHAR(64) DEFAULT 'NORMAL';
ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS biz_scope VARCHAR(64);
ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS dataset_mode VARCHAR(32) NOT NULL DEFAULT 'SINGLE';
ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS default_dataset_id VARCHAR(64);
ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS dataset_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS metadata_json TEXT;

CREATE TABLE IF NOT EXISTS agi_knowledge_dataset (
    id                  VARCHAR(64) PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL,
    knowledge_base_id   VARCHAR(64) NOT NULL REFERENCES agi_knowledge_base(id) ON DELETE CASCADE,
    name                VARCHAR(300) NOT NULL,
    code                VARCHAR(100),
    description         TEXT,
    dataset_type        VARCHAR(64) NOT NULL,
    biz_type            VARCHAR(64),
    biz_id              VARCHAR(64),
    topic_id            VARCHAR(64),
    topic_title         VARCHAR(300),
    security_level      VARCHAR(64),
    retention_period    VARCHAR(64),
    owner_unit_id       VARCHAR(64),
    source_system       VARCHAR(64),
    source_version      VARCHAR(100),
    document_count      INTEGER NOT NULL DEFAULT 0,
    chunk_count         INTEGER NOT NULL DEFAULT 0,
    source_count        INTEGER NOT NULL DEFAULT 0,
    index_status        VARCHAR(32) NOT NULL DEFAULT 'NOT_INDEXED',
    last_sync_time      TIMESTAMPTZ,
    last_index_time     TIMESTAMPTZ,
    metadata_json       TEXT,
    status              VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_by          VARCHAR(64),
    updated_by          VARCHAR(64),
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_knowledge_source_index (
    id                      VARCHAR(64) PRIMARY KEY,
    tenant_id               VARCHAR(64) NOT NULL,
    knowledge_base_id       VARCHAR(64) NOT NULL REFERENCES agi_knowledge_base(id) ON DELETE CASCADE,
    dataset_id              VARCHAR(64) NOT NULL REFERENCES agi_knowledge_dataset(id) ON DELETE CASCADE,
    topic_id                VARCHAR(64),
    source_system           VARCHAR(64) NOT NULL,
    source_type             VARCHAR(64) NOT NULL,
    source_ref_id           VARCHAR(64) NOT NULL,
    material_source_type    VARCHAR(64),
    material_type           VARCHAR(64),
    source_archive_file_id  VARCHAR(64),
    source_title_snapshot   VARCHAR(500),
    source_version          VARCHAR(100),
    source_url              VARCHAR(1000),
    storage_path            VARCHAR(512),
    metadata_snapshot       TEXT,
    document_id             VARCHAR(64),
    index_status            VARCHAR(32) NOT NULL DEFAULT 'NOT_INDEXED',
    last_sync_time          TIMESTAMPTZ,
    last_index_time         TIMESTAMPTZ,
    error_message           TEXT,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL
);

ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS dataset_id VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS source_index_id VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS topic_id VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS doc_type VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS source_system VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS source_type VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS source_ref_id VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS material_source_type VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS material_type VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS source_archive_file_id VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS source_version VARCHAR(100);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS title_snapshot VARCHAR(500);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS metadata_json TEXT;
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS summary_text TEXT;
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS security_level VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS owner_unit_id VARCHAR(64);
ALTER TABLE agi_knowledge_document ADD COLUMN IF NOT EXISTS last_index_time TIMESTAMPTZ;

ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS dataset_id VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS source_index_id VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS topic_id VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS chunk_title VARCHAR(300);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS chunk_type VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS source_system VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS source_type VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS source_ref_id VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS material_source_type VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS material_type VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS source_archive_file_id VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS source_page VARCHAR(50);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS source_position VARCHAR(200);
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS citation_text TEXT;
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS metadata_json TEXT;
ALTER TABLE agi_knowledge_chunk ADD COLUMN IF NOT EXISTS security_level VARCHAR(64);

CREATE TABLE IF NOT EXISTS agi_knowledge_event (
    id                      VARCHAR(64) PRIMARY KEY,
    tenant_id               VARCHAR(64) NOT NULL,
    knowledge_base_id       VARCHAR(64) NOT NULL,
    dataset_id              VARCHAR(64) NOT NULL,
    topic_id                VARCHAR(64),
    source_index_id         VARCHAR(64),
    document_id             VARCHAR(64),
    chunk_id                VARCHAR(64),
    source_ref_id           VARCHAR(64),
    source_archive_file_id  VARCHAR(64),
    event_date              VARCHAR(50),
    event_title             VARCHAR(300),
    event_description       TEXT,
    event_type              VARCHAR(64),
    project_stage           VARCHAR(100),
    confidence              NUMERIC(5, 2),
    need_confirm            BOOLEAN NOT NULL DEFAULT FALSE,
    confirm_status          VARCHAR(32) DEFAULT 'PENDING',
    citation_json           TEXT,
    metadata_json           TEXT,
    created_at              TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_knowledge_structured_record (
    id                      VARCHAR(64) PRIMARY KEY,
    tenant_id               VARCHAR(64) NOT NULL,
    knowledge_base_id       VARCHAR(64) NOT NULL,
    dataset_id              VARCHAR(64) NOT NULL,
    topic_id                VARCHAR(64),
    source_index_id         VARCHAR(64),
    source_ref_id           VARCHAR(64),
    source_archive_file_id  VARCHAR(64),
    record_type             VARCHAR(64) NOT NULL,
    record_title            VARCHAR(300),
    record_date             VARCHAR(50),
    record_json             TEXT NOT NULL,
    source_document_id      VARCHAR(64),
    source_chunk_id         VARCHAR(64),
    confirm_status          VARCHAR(32) DEFAULT 'PENDING',
    metadata_json           TEXT,
    created_at              TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_kb_dataset_base ON agi_knowledge_dataset(knowledge_base_id, status, updated_at);
CREATE INDEX IF NOT EXISTS idx_kb_dataset_topic ON agi_knowledge_dataset(topic_id);
CREATE INDEX IF NOT EXISTS idx_source_dataset ON agi_knowledge_source_index(knowledge_base_id, dataset_id, index_status);
CREATE INDEX IF NOT EXISTS idx_source_ref ON agi_knowledge_source_index(source_system, source_type, source_ref_id);
CREATE INDEX IF NOT EXISTS idx_doc_dataset ON agi_knowledge_document(knowledge_base_id, dataset_id, created_at);
CREATE INDEX IF NOT EXISTS idx_chunk_dataset ON agi_knowledge_chunk(knowledge_base_id, dataset_id, document_id, chunk_index);

INSERT INTO agi_knowledge_dataset (
    id, tenant_id, knowledge_base_id, name, dataset_type, document_count, chunk_count, source_count,
    index_status, status, created_at, updated_at
)
SELECT
    'ds_default_' || kb.id,
    kb.tenant_id,
    kb.id,
    '默认数据集',
    'DEFAULT',
    kb.document_count,
    kb.chunk_count,
    0,
    'READY',
    'ENABLED',
    kb.created_at,
    kb.updated_at
FROM agi_knowledge_base kb
WHERE kb.default_dataset_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM agi_knowledge_dataset ds WHERE ds.id = 'ds_default_' || kb.id);

UPDATE agi_knowledge_base kb
SET default_dataset_id = 'ds_default_' || kb.id,
    dataset_count = CASE WHEN dataset_count = 0 THEN 1 ELSE dataset_count END,
    kb_type = COALESCE(kb_type, 'NORMAL'),
    dataset_mode = COALESCE(dataset_mode, 'SINGLE')
WHERE default_dataset_id IS NULL;

UPDATE agi_knowledge_document d
SET dataset_id = 'ds_default_' || d.knowledge_base_id
WHERE dataset_id IS NULL;

UPDATE agi_knowledge_chunk c
SET dataset_id = 'ds_default_' || c.knowledge_base_id
WHERE dataset_id IS NULL;
