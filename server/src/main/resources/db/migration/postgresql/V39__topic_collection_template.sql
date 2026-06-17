INSERT INTO agi_generation_template (
    id, tenant_id, name, code, description, category, owner_unit_id, output_type,
    template_category, docx_config, layout_config, template_schema, workflow_id, workflow_snapshot,
    status, version, created_by, created_at, updated_at
) VALUES (
    'template_research_topic_collection_001',
    'tenant_default',
    '专题汇编编研模板',
    'research_topic_collection',
    '基于 DOCX 母版的档案专题汇编成果',
    'RESEARCH',
    'unit_default',
    'DOCX',
    'topic_collection',
    $${"masterFile":"research/docx-masters/archive_topic_collection.docx","templateType":"archive_topic_collection"}$$,
    $${"defaultTab":"docx","enableHtmlPreview":false}$$,
    $${
  "title": "专题汇编编研模板",
  "variables": [
    { "name": "topic", "label": "主题", "type": "string", "required": true }
  ],
  "sections": [
    { "key": "background", "title": "一、专题背景", "instruction": "说明专题形成背景。", "citationRequired": true },
    { "key": "scope", "title": "二、资料范围与编排说明", "instruction": "说明资料来源与编排方式。", "citationRequired": true },
    { "key": "core_documents", "title": "三、核心文件汇编", "instruction": "输出核心文件条目。", "outputFormat": "document_collection", "citationRequired": true },
    { "key": "gallery", "title": "六、图片与实物档案", "instruction": "输出图片说明。", "outputFormat": "gallery", "citationRequired": true },
    { "key": "interpretation", "title": "七、资料解读", "instruction": "提炼主题价值。", "citationRequired": false }
  ]
}$$,
    'workflow_research_mvp',
    NULL,
    'ENABLED',
    1,
    'user_admin',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
