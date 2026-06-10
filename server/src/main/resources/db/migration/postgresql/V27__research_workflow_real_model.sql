UPDATE agi_workflow_version
SET definition_json = REPLACE(REPLACE(definition_json, '"model":"research-stub"', '"model":""'), '"model": "research-stub"', '"model": ""')
WHERE workflow_id = 'workflow_research_mvp'
  AND definition_json LIKE '%research-stub%';
