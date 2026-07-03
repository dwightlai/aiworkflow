package com.mw.ai.agi.knowledge.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownStructureParserTest {

    private final MarkdownStructureParser parser = new MarkdownStructureParser();

    @Test
    void preservesFullPathForSameNamedHeadings() {
        DocumentStructure document = parser.parse(
                "manual.md",
                "# 运维手册\n## 测试环境\n### 访问地址\nhttp://test\n## 生产环境\n### 访问地址\nhttp://prod"
        );

        assertThat(document.nodes())
                .filteredOn(node -> node.text().contains("http://prod"))
                .singleElement()
                .extracting(DocumentNode::sectionPath)
                .isEqualTo(List.of("运维手册", "生产环境", "访问地址"));
    }

    @Test
    void keepsFencedCodeAsOneAtomicNode() {
        DocumentStructure document = parser.parse(
                "api.md",
                "# API\n```json\n{\n  \"chunkSize\": 800,\n  \"overlap\": 80\n}\n```\n说明"
        );

        assertThat(document.nodes())
                .filteredOn(node -> node.type() == NodeType.CODE_BLOCK)
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.text()).contains("\"chunkSize\": 800", "\"overlap\": 80");
                    assertThat(node.metadata()).containsEntry("atomic", true);
                });
    }

    @Test
    void recognizesExplicitFaqPair() {
        DocumentStructure document = parser.parse(
                "faq.md",
                "# FAQ\nQ: 什么是父块？\nA: 父块用于承载完整上下文。\n\nQ: 什么是子块？\nA: 子块用于精准召回。"
        );

        assertThat(document.nodes())
                .filteredOn(node -> node.type() == NodeType.FAQ)
                .extracting(DocumentNode::text)
                .containsExactly(
                        "Q: 什么是父块？\nA: 父块用于承载完整上下文。",
                        "Q: 什么是子块？\nA: 子块用于精准召回。"
                );
    }

    @Test
    void recognizesMarkdownTableAsOneStructuredNode() {
        DocumentStructure document = parser.parse(
                "table.md",
                "# 台账\n| 名称 | 数量 |\n|---|---:|\n| 服务器 | 1 |\n| UPS | 2 |"
        );

        assertThat(document.nodes())
                .filteredOn(node -> node.type() == NodeType.TABLE)
                .singleElement()
                .satisfies(node -> assertThat(node.text()).contains("名称", "服务器", "UPS"));
    }
}
