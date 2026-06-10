CREATE TABLE IF NOT EXISTS agi_generation_template (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(64) NOT NULL,
    owner_unit_id VARCHAR(64),
    output_type VARCHAR(32) NOT NULL,
    template_schema TEXT,
    workflow_id VARCHAR(64),
    workflow_snapshot TEXT,
    status VARCHAR(32) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    created_by VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_generation_template_tenant ON agi_generation_template(tenant_id, category);

CREATE TABLE IF NOT EXISTS agi_generation_job (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    bot_id VARCHAR(64),
    template_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64),
    unit_id VARCHAR(64),
    user_id VARCHAR(64),
    knowledge_base_ids TEXT,
    external_corpus_ref TEXT,
    variables TEXT,
    status VARCHAR(32) NOT NULL,
    outline_json TEXT,
    section_outputs_json TEXT,
    workflow_run_snapshot TEXT,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_generation_job_tenant ON agi_generation_job(tenant_id, created_at);

CREATE TABLE IF NOT EXISTS agi_generation_output (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    output_type VARCHAR(32) NOT NULL,
    content_markdown TEXT,
    citations TEXT,
    source_snapshot TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_generation_output_job ON agi_generation_output(job_id);

INSERT INTO agi_generation_template (
    id, tenant_id, name, code, description, category, owner_unit_id, output_type,
    template_schema, workflow_id, workflow_snapshot, status, version, created_by, created_at, updated_at
) VALUES (
    'template_research_001',
    'tenant_default',
    '专题编研成果模板',
    'research_report',
    '数字档案馆智能编研MVP模板',
    'RESEARCH',
    'unit_default',
    'MARKDOWN',
    $${
  "title": "专题编研成果模板",
  "variables": [
    { "name": "topic", "label": "主题", "type": "string", "required": true },
    { "name": "audience", "label": "面向对象", "type": "string", "required": false }
  ],
  "sections": [
    {
      "key": "overview",
      "title": "一、背景概述",
      "instruction": "根据资料概括主题背景，不超过800字。",
      "requiredSources": ["INTERNAL_KNOWLEDGE_BASE", "EXTERNAL_CORPUS"],
      "citationRequired": true
    },
    {
      "key": "timeline",
      "title": "二、发展脉络",
      "instruction": "按时间顺序梳理关键事件。",
      "outputFormat": "timeline",
      "citationRequired": true
    },
    {
      "key": "conclusion",
      "title": "三、总结建议",
      "instruction": "结合资料形成总结，不得编造事实。",
      "citationRequired": false
    }
  ]
}$$,
    'workflow_research_mvp',
    $${
  "code": "archive_research_generation_flow_mvp",
  "name": "档案智能编研MVP工作流",
  "nodes": [
    { "id": "n1", "type": "START", "name": "接收请求", "key": "receive_request" },
    { "id": "n2", "type": "CONDITION", "name": "权限校验", "key": "auth_check" },
    { "id": "n3", "type": "DATABASE", "name": "读取模板", "key": "load_template" },
    { "id": "n4", "type": "CONNECTOR", "name": "读取主题库资料", "key": "load_theme_corpus" },
    { "id": "n5", "type": "RAG", "name": "检索知识库", "key": "search_knowledge" },
    { "id": "n6", "type": "LLM", "name": "生成大纲", "key": "generate_outline" },
    { "id": "n7", "type": "LOOP", "name": "分章节生成正文", "key": "generate_sections" },
    { "id": "n8", "type": "DATABASE", "name": "保存成果和引用", "key": "save_output" }
  ],
  "edges": [
    { "from": "n1", "to": "n2" },
    { "from": "n2", "to": "n3" },
    { "from": "n3", "to": "n4" },
    { "from": "n4", "to": "n5" },
    { "from": "n5", "to": "n6" },
    { "from": "n6", "to": "n7" },
    { "from": "n7", "to": "n8" }
  ]
}$$,
    'ENABLED',
    1,
    'user_admin',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
