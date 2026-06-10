package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationTemplate;
import com.mw.ai.agi.generation.persistence.GenerationTemplateEntity;
import com.mw.ai.agi.generation.persistence.GenerationTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class InMemoryGenerationTemplateStore implements GenerationTemplateStore {
    private static final String SEED_TEMPLATE_SCHEMA = """
            {
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
            }
            """;
    private static final String SEED_WORKFLOW_SNAPSHOT = """
            {
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
            }
            """;
    private final List<GenerationTemplate> templates = new java.util.concurrent.CopyOnWriteArrayList<>();

    public InMemoryGenerationTemplateStore() {
        Instant now = Instant.parse("2026-06-01T00:00:00Z");
        templates.add(new GenerationTemplate(
                "template_research_001",
                "tenant_default",
                "专题编研成果模板",
                "research_report",
                "数字档案馆智能编研模板",
                "RESEARCH",
                "unit_default",
                "MARKDOWN",
                SEED_TEMPLATE_SCHEMA,
                "workflow_research_mvp",
                SEED_WORKFLOW_SNAPSHOT,
                "ENABLED",
                1,
                "user_admin",
                now,
                now
        ));
    }

    @Override
    public GenerationTemplate save(GenerationTemplate template) {
        templates.removeIf(current -> current.id().equals(template.id()));
        templates.add(template);
        return template;
    }

    @Override
    public Optional<GenerationTemplate> findById(String id) {
        return templates.stream().filter(template -> template.id().equals(id)).findFirst();
    }

    @Override
    public List<GenerationTemplate> list(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return new java.util.ArrayList<>(templates);
        }
        return templates.stream().filter(template -> tenantId.equals(template.tenantId())).toList();
    }

    @Override
    public void delete(String id) {
        templates.removeIf(template -> template.id().equals(id));
    }
}
