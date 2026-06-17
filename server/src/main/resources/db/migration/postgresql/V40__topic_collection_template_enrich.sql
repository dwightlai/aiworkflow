UPDATE agi_generation_template
SET
    docx_config = $${"masterFile":"research/docx-masters/archive_topic_collection.docx","templateType":"archive_topic_collection"}$$,
    layout_config = $${"defaultTab":"docx","enableHtmlPreview":false}$$,
    template_schema = $${
  "title": "专题汇编编研模板",
  "variables": [
    { "name": "topic", "label": "主题", "type": "string", "required": true }
  ],
  "sections": [
    { "key": "background", "title": "一、专题背景", "instruction": "说明专题形成背景、业务意义和编研目的。", "citationRequired": true },
    { "key": "scope", "title": "二、资料范围与编排说明", "instruction": "说明资料来源、时间范围、筛选标准和编排方式。", "citationRequired": true },
    { "key": "core_documents", "title": "三、核心文件汇编", "instruction": "根据档案馆资料输出核心文件条目，每条含题名、档号、形成时间、责任单位、摘要。", "outputFormat": "document_collection", "citationRequired": true },
    { "key": "gallery", "title": "六、图片与实物档案", "instruction": "输出图片展品，每条含题名、图片引用、说明。", "outputFormat": "gallery", "citationRequired": true },
    { "key": "interpretation", "title": "七、资料解读", "instruction": "提炼主题价值、业务特点和历史意义。", "outputFormat": "analysis", "citationRequired": false }
  ]
}$$,
    workflow_snapshot = $${
  "code": "archive_research_generation_flow_mvp",
  "name": "档案智能编研MVP工作流",
  "nodes": [
    { "id": "n1", "type": "START", "name": "接收编研请求", "order": 1 },
    { "id": "n2", "type": "CONDITION", "name": "参数校验", "order": 2 },
    { "id": "n3", "type": "HTTP_TOOL", "name": "读取编研模板", "order": 3 },
    { "id": "n4", "type": "HTTP_TOOL", "name": "读取档案馆资料", "order": 4 },
    { "id": "n5", "type": "KNOWLEDGE_RETRIEVAL", "name": "检索知识库", "order": 5 },
    { "id": "n6", "type": "LLM", "name": "生成大纲", "order": 6 },
    { "id": "n7", "type": "LOOP", "name": "分章生成正文", "order": 7 },
    { "id": "n9", "type": "END", "name": "输出编研成果", "order": 8 }
  ]
}$$,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'template_research_topic_collection_001';
