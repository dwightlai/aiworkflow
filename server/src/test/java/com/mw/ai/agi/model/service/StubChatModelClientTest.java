package com.mw.ai.agi.model.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubChatModelClientTest {
    private final StubChatModelClient client = new StubChatModelClient();

    @Test
    void generatesOutlineForOutlinePrompt() {
        String result = client.generate("model_research_chat", "research-stub", """
                编研主题：电子文件归档
                章节结构：
                - 一、背景概述
                请输出 Markdown 格式的编研大纲
                """, null);
        assertThat(result).contains("编研大纲").contains("背景概述");
        assertThat(result).doesNotContain("Stub 模型输出");
    }

    @Test
    void generatesSectionEvenWhenOutlineReferencePresent() {
        String result = client.generate("model_research_chat", "research-stub", """
                编研主题：电子文件归档
                章节标题：一、背景概述
                编写要求：根据资料概括主题背景

                档案馆资料：
                - 关于加强电子文件归档管理的通知：2024年1月发布

                编研大纲参考：
                # 电子文件归档编研大纲
                ## 一、背景概述

                请撰写本章节的 Markdown 正文
                """, null);
        assertThat(result).startsWith("## 一、背景概述");
        assertThat(result).contains("电子文件归档");
        assertThat(result).contains("关于加强电子文件归档管理的通知");
        assertThat(result).doesNotContain("## 二、发展脉络");
    }
}
