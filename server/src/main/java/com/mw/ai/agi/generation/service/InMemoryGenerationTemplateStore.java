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
                "DOCX",
                "report",
                """
                {"showCover":true,"showToc":true,"tocDepth":2,"showPageNumber":true,"showReferenceSection":true,"lineSpacingPt":28}
                """,
                """
                {"defaultTab":"docx","enableHtmlPreview":true}
                """,
                null,
                SEED_TEMPLATE_SCHEMA,
                "workflow_research_mvp",
                SEED_WORKFLOW_SNAPSHOT,
                "ENABLED",
                1,
                "user_admin",
                "user_admin",
                now,
                now
        ));
        templates.add(new GenerationTemplate(
                "template_research_topic_collection_001",
                "tenant_default",
                "专题汇编编研模板",
                "research_topic_collection",
                "基于 DOCX 母版的档案专题汇编成果，绑定 workflow_research_mvp",
                "RESEARCH",
                "unit_default",
                "DOCX",
                "topic_collection",
                """
                {"masterFile":"research/docx-masters/archive_topic_collection.docx","templateType":"archive_topic_collection"}
                """,
                """
                {"defaultTab":"docx","enableHtmlPreview":false}
                """,
                null,
                """
                {
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
                }
                """,
                "workflow_research_mvp",
                SEED_WORKFLOW_SNAPSHOT,
                "ENABLED",
                1,
                "user_admin",
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
