package com.mw.ai.agi.model.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StubChatModelClient implements ChatModelClient {
    private static final Pattern TOPIC_PATTERN = Pattern.compile("编研主题[：:]([^\\n]+)");
    private static final Pattern SECTION_TITLE_PATTERN = Pattern.compile("章节标题[：:]([^\\n]+)");
    private static final Pattern SECTION_INSTRUCTION_PATTERN = Pattern.compile("编写要求[：:]([^\\n]+)");
    private static final Pattern CORPUS_PATTERN = Pattern.compile("档案馆资料[：:]([\\s\\S]*?)(?:\\n\\n知识库内容|$)");

    @Override
    public String generate(String providerId, String model, String prompt, Map<String, Object> options) {
        if (prompt == null || prompt.isBlank()) {
            return "（模型未返回内容）";
        }
        if (isSectionPrompt(prompt)) {
            return generateSection(prompt);
        }
        if (isOutlinePrompt(prompt)) {
            return generateOutline(prompt);
        }
        return "基于所提供资料生成的编研内容。";
    }

    private boolean isSectionPrompt(String prompt) {
        return prompt.contains("撰写本章节")
                || prompt.contains("章节标题")
                || (prompt.contains("编写要求") && prompt.contains("章节"));
    }

    private boolean isOutlinePrompt(String prompt) {
        return prompt.contains("输出 Markdown 格式的编研大纲")
                || (prompt.contains("编研大纲") && prompt.contains("章节结构"));
    }

    private String generateOutline(String prompt) {
        String topic = extract(TOPIC_PATTERN, prompt, "专题编研").trim();
        return """
                # %s编研大纲

                ## 一、背景概述
                - 梳理主题政策背景与制度依据
                - 结合档案馆资料说明现状与问题

                ## 二、发展脉络
                - 按时间线归纳关键节点
                - 标注主要文件与责任主体

                ## 三、总结建议
                - 归纳主要经验与不足
                - 提出可执行的改进建议
                """.formatted(topic).trim();
    }

    private String generateSection(String prompt) {
        String title = extract(SECTION_TITLE_PATTERN, prompt, "本章节").trim();
        String instruction = extract(SECTION_INSTRUCTION_PATTERN, prompt, "围绕主题撰写正文").trim();
        String topic = extract(TOPIC_PATTERN, prompt, "专题编研").trim();
        String corpus = extract(CORPUS_PATTERN, prompt, "暂无档案馆资料摘要。").trim();
        if (corpus.length() > 400) {
            corpus = corpus.substring(0, 400) + "...";
        }
        return """
                ## %s

                围绕「%s」，%s

                依据档案馆资料：
                %s

                结合知识库检索结果，本节从制度背景、实践做法和存在问题三个层面展开论述，力求观点有据、表述规范。

                > 注：当前为 Stub 模型输出。在模型管理中配置真实大模型后，将生成完整编研正文。
                """.formatted(title, topic, instruction, corpus).trim();
    }

    private String extract(Pattern pattern, String prompt, String defaultValue) {
        Matcher matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }
}
