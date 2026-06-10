UPDATE agi_workflow_version
SET definition_json = REPLACE(
    definition_json,
    '/api/generation-templates/template_research_001/runtime',
    '/api/generation-templates/{{templateId}}/runtime'
)
WHERE workflow_id = 'workflow_research_mvp'
  AND definition_json LIKE '%/api/generation-templates/template_research_001/runtime%';
