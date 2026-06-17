ALTER TABLE agi_generation_template
    ADD COLUMN IF NOT EXISTS template_category VARCHAR(64),
    ADD COLUMN IF NOT EXISTS docx_config TEXT,
    ADD COLUMN IF NOT EXISTS layout_config TEXT,
    ADD COLUMN IF NOT EXISTS linked_html_template_id VARCHAR(64);

ALTER TABLE agi_generation_output
    ADD COLUMN IF NOT EXISTS content_json TEXT,
    ADD COLUMN IF NOT EXISTS output_template_id VARCHAR(64);

UPDATE agi_generation_template
SET output_type = 'DOCX',
    template_category = 'report',
    docx_config = $${"showCover":true,"showToc":true,"tocDepth":2,"showPageNumber":true,"showReferenceSection":true,"lineSpacingPt":28}$$,
    layout_config = $${"defaultTab":"docx","enableHtmlPreview":true}$$
WHERE id = 'template_research_001';

INSERT INTO agi_generation_template (
    id, tenant_id, name, code, description, category, owner_unit_id, output_type,
    template_category, docx_config, layout_config, template_schema, workflow_id, workflow_snapshot,
    status, version, created_by, created_at, updated_at
) VALUES (
    'template_research_gallery_001',
    'tenant_default',
    '图文展陈编研模板',
    'research_gallery',
    '图文并茂的档案展陈编研成果',
    'RESEARCH',
    'unit_default',
    'DOCX',
    'gallery',
    $${"showCover":true,"showToc":true,"showReferenceSection":true}$$,
    $${"defaultTab":"docx","enableHtmlPreview":true}$$,
    $${
  "title": "图文展陈编研模板",
  "variables": [
    { "name": "topic", "label": "主题", "type": "string", "required": true }
  ],
  "sections": [
    { "key": "intro", "title": "一、专题背景", "instruction": "概括专题背景。", "citationRequired": true },
    { "key": "gallery", "title": "二、重点档案展品", "instruction": "按展品输出图片说明与档号。", "outputFormat": "gallery", "citationRequired": true },
    { "key": "summary", "title": "三、总结", "instruction": "形成总结。", "citationRequired": false }
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

INSERT INTO agi_generation_template (
    id, tenant_id, name, code, description, category, owner_unit_id, output_type,
    template_category, docx_config, layout_config, template_schema, workflow_id, workflow_snapshot,
    status, version, created_by, created_at, updated_at
) VALUES (
    'template_research_timeline_001',
    'tenant_default',
    '时间轴专题编研模板',
    'research_timeline',
    '按时间脉络梳理的编研成果',
    'RESEARCH',
    'unit_default',
    'DOCX',
    'timeline',
    $${"showCover":true,"showToc":true,"showReferenceSection":true}$$,
    $${"defaultTab":"docx","enableHtmlPreview":true}$$,
    $${
  "title": "时间轴专题编研模板",
  "variables": [
    { "name": "topic", "label": "主题", "type": "string", "required": true }
  ],
  "sections": [
    { "key": "overview", "title": "一、背景概述", "instruction": "概括主题背景。", "citationRequired": true },
    { "key": "timeline", "title": "二、发展脉络", "instruction": "按时间顺序梳理关键事件。", "outputFormat": "timeline", "citationRequired": true },
    { "key": "conclusion", "title": "三、总结建议", "instruction": "形成总结。", "citationRequired": false }
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
