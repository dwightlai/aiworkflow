INSERT INTO agi_workflow (
    id, tenant_id, owner_unit_id, name, description, status, current_version_id, created_by, created_at, updated_at
) VALUES (
    'workflow_research_mvp',
    'tenant_default',
    'org_default_unit',
    '档案智能编研MVP工作流',
    '接收请求、权限校验、读取模板、主题库、知识库检索、生成大纲、分章生成、保存成果',
    'PUBLISHED',
    NULL,
    'user_admin',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO agi_workflow_version (
    id, workflow_id, version, definition_json, status, published_by, published_at, created_at
) VALUES (
    'wfver_research_mvp_1',
    'workflow_research_mvp',
    1,
    $${
  "nodes": [
    { "id": "n1", "type": "START", "name": "接收请求", "config": {} },
    { "id": "n2", "type": "CONDITION", "name": "权限校验", "config": {} },
    { "id": "n3", "type": "TEXT_TRANSFORM", "name": "读取模板", "config": { "outputKey": "template" } },
    { "id": "n4", "type": "HTTP_TOOL", "name": "读取主题库资料", "config": { "outputKey": "corpus" } },
    { "id": "n5", "type": "KNOWLEDGE_RETRIEVAL", "name": "检索知识库", "config": { "outputKey": "knowledge" } },
    { "id": "n6", "type": "LLM", "name": "生成大纲", "config": { "outputKey": "outline" } },
    { "id": "n7", "type": "LOOP", "name": "分章节生成正文", "config": { "outputKey": "sections" } },
    { "id": "n8", "type": "END", "name": "保存成果和引用", "config": { "outputKeys": ["output"] } }
  ],
  "edges": [
    { "id": "e1", "sourceNodeId": "n1", "targetNodeId": "n2", "condition": null },
    { "id": "e2", "sourceNodeId": "n2", "targetNodeId": "n3", "condition": null },
    { "id": "e3", "sourceNodeId": "n3", "targetNodeId": "n4", "condition": null },
    { "id": "e4", "sourceNodeId": "n4", "targetNodeId": "n5", "condition": null },
    { "id": "e5", "sourceNodeId": "n5", "targetNodeId": "n6", "condition": null },
    { "id": "e6", "sourceNodeId": "n6", "targetNodeId": "n7", "condition": null },
    { "id": "e7", "sourceNodeId": "n7", "targetNodeId": "n8", "condition": null }
  ],
  "variables": []
}$$,
    'PUBLISHED',
    'user_admin',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

UPDATE agi_workflow
SET current_version_id = 'wfver_research_mvp_1'
WHERE id = 'workflow_research_mvp';

UPDATE agi_generation_template
SET workflow_id = 'workflow_research_mvp',
    workflow_snapshot = NULL
WHERE id = 'template_research_001';
