package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchContentJsonBuilderTopicCollectionTest {
    private final ResearchContentJsonBuilder builder = new ResearchContentJsonBuilder(new ObjectMapper());

    @Test
    void buildsTopicCollectionContentJsonWithCorpusFallback() {
        List<Map<String, Object>> sections = List.of(
                Map.of("key", "background", "title", "一、专题背景"),
                Map.of("key", "core_documents", "title", "三、核心文件汇编", "outputFormat", "document_collection"),
                Map.of("key", "interpretation", "title", "七、资料解读", "outputFormat", "analysis")
        );
        List<Map<String, Object>> sectionOutputs = List.of(
                Map.of("contentMarkdown", "## 一、专题背景\n说明背景。"),
                Map.of("contentMarkdown", ""),
                Map.of("contentMarkdown", "## 七、资料解读\n提炼价值。")
        );
        List<Map<String, Object>> corpusItems = List.of(
                Map.of(
                        "title", "档案数字化建设方案",
                        "archiveCode", "2021-XX-001",
                        "formationDate", "2021-03-12",
                        "responsibleUnit", "档案中心",
                        "summary", "明确数字化建设目标。"
                )
        );

        Map<String, Object> content = builder.build(
                "topic_collection",
                "档案数字化建设专题资料汇编",
                "档案数字化建设",
                "档案管理人员",
                sections,
                sectionOutputs,
                List.of(),
                corpusItems
        );

        assertThat(content.get("templateType")).isEqualTo("archive_topic_collection");
        assertThat(content.get("cover")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> builtSections = (List<Map<String, Object>>) content.get("sections");
        Map<String, Object> coreDocuments = builtSections.stream()
                .filter(section -> "core_documents".equals(section.get("id")))
                .findFirst()
                .orElseThrow();
        assertThat(coreDocuments.get("type")).isEqualTo("document_collection");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) coreDocuments.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("archiveCode")).isEqualTo("2021-XX-001");
    }
}
