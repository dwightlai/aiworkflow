package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowEdge;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResearchWorkflowDefinitionBuilder {
    public static final String WORKFLOW_ID = "workflow_research_mvp";
    public static final String WORKFLOW_VERSION_ID = "wfver_research_mvp_1";
    public static final String MODEL_PROVIDER_ID = "model_research_chat";
    public static final String DEFAULT_TEMPLATE_ID = "template_research_001";

    private ResearchWorkflowDefinitionBuilder() {
    }

    public static WorkflowDefinition build() {
        return new WorkflowDefinition(
                List.of(
                        node("n1", WorkflowNodeType.START, "接收编研请求", 80, 80, Map.of(
                                "inputParams", List.of(
                                        Map.of("name", "templateId", "type", "String", "required", false),
                                        Map.of("name", "topic", "type", "String", "required", true),
                                        Map.of("name", "audience", "type", "String", "required", false),
                                        Map.of("name", "themeLibraryId", "type", "String", "required", false)
                                ),
                                "defaultInputJson", """
                                        {"templateId":"template_research_001","topic":"","audience":"档案管理人员","themeLibraryId":"theme_001"}
                                        """.trim()
                        )),
                        node("n2", WorkflowNodeType.CONDITION, "参数校验", 80, 200, Map.of(
                                "contextKey", "topic",
                                "operator", "IS_NOT_EMPTY",
                                "trueTargetNodeId", "n3",
                                "falseTargetNodeId", "n3"
                        )),
                        node("n3", WorkflowNodeType.HTTP_TOOL, "HTTP 读取专题编研成果模板", 80, 320, Map.of(
                                "method", "GET",
                                "url", "{{baseUrl}}/api/generation-templates/{{templateId}}/runtime",
                                "headers", List.of(Map.of("key", "Accept", "value", "application/json")),
                                "responseBodyType", "JSON",
                                "timeoutMs", 30000,
                                "outputKey", "templateHttp"
                        )),
                        node("n4", WorkflowNodeType.HTTP_TOOL, "HTTP 读取档案馆资料", 80, 440, Map.of(
                                "method", "GET",
                                "url", "{{baseUrl}}/api/research/mock/theme-libraries/{{themeLibraryId}}/corpus",
                                "headers", List.of(Map.of("key", "Accept", "value", "application/json")),
                                "responseBodyType", "JSON",
                                "timeoutMs", 30000,
                                "outputKey", "corpusHttp"
                        )),
                        node("n5", WorkflowNodeType.KNOWLEDGE_RETRIEVAL, "检索知识库", 80, 560, Map.of(
                                "queryKey", "topic",
                                "queryText", "{{topic}}",
                                "knowledgeBaseIds", List.of(),
                                "topK", 3,
                                "fetchCount", 3,
                                "outputKey", "knowledge",
                                "timeoutSeconds", 30,
                                "retryCount", 0,
                                "errorStrategy", "INTERRUPT_NODE"
                        )),
                        node("n6", WorkflowNodeType.LLM, "大模型生成大纲", 80, 680, llmConfig(
                                "outlineText",
                                "你是档案编研专家。根据模板章节、档案馆资料和知识库片段生成结构清晰、引用有据的编研大纲，不得编造不存在的事实。",
                                """
                                        编研主题：{{topic}}
                                        面向对象：{{audience}}
                                        模板名称：{{templateHttp.body.data.name}}
                                        章节结构：
                                        {{templateHttp.body.data.outlineSections}}

                                        档案馆资料摘要：
                                        {{corpusHttp.body.data.summary}}

                                        知识库检索结果：
                                        {{knowledge.content}}

                                        请输出 Markdown 格式的编研大纲，包含各章节标题和每节写作要点（2-3句）。"""
                        )),
                        node("n7", WorkflowNodeType.LOOP, "大模型分章生成正文", 80, 800, Map.of(
                                "loopVar", "templateHttp.body.data.sections",
                                "itemVar", "section",
                                "indexVar", "sectionIndex",
                                "outputKey", "sectionOutputs",
                                "maxIterations", 20,
                                "loopSteps", List.of(llmLoopStep())
                        )),
                        node("n9", WorkflowNodeType.END, "输出编研成果", 80, 920, Map.of(
                                "outputParams", List.of(
                                        param("topic", "topic"),
                                        param("audience", "audience"),
                                        param("generationTemplateId", "templateHttp.body.data.id"),
                                        param("generationTemplateName", "templateHttp.body.data.name"),
                                        param("outputType", "templateHttp.body.data.outputType"),
                                        param("templateSections", "templateHttp.body.data.sections"),
                                        param("outlineText", "outlineText"),
                                        param("sectionOutputs", "sectionOutputs"),
                                        param("corpusItems", "corpusHttp.body.data.items"),
                                        param("corpusSummary", "corpusHttp.body.data.summary"),
                                        param("knowledge", "knowledge")
                                )
                        ))
                ),
                List.of(
                        edge("e1", "n1", "n2"),
                        edge("e2", "n2", "n3"),
                        edge("e3", "n3", "n4"),
                        edge("e4", "n4", "n5"),
                        edge("e5", "n5", "n6"),
                        edge("e6", "n6", "n7"),
                        edge("e7", "n7", "n9")
                ),
                List.of()
        );
    }

    private static Map<String, Object> llmConfig(String outputKey, String systemPrompt, String userPrompt) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("providerId", MODEL_PROVIDER_ID);
        config.put("systemPrompt", systemPrompt);
        config.put("userPrompt", userPrompt);
        config.put("outputKey", outputKey);
        config.put("temperature", 0.3);
        config.put("topP", 0.9);
        config.put("maxTokens", 3000);
        config.put("timeoutSeconds", 120);
        config.put("retryCount", 1);
        config.put("errorStrategy", "INTERRUPT_NODE");
        config.put("outputParams", List.of(Map.of("name", outputKey, "type", "String")));
        return config;
    }

    private static Map<String, Object> llmLoopStep() {
        Map<String, Object> step = llmConfig(
                "sectionMarkdown",
                "你是档案编研写作助手。根据章节要求、档案馆资料和知识库内容撰写正文，语言规范、逻辑清楚，有引用处标注来源，不得编造事实。",
                """
                        编研主题：{{topic}}
                        章节标题：{{section.title}}
                        编写要求：{{section.instruction}}

                        档案馆资料：
                        {{corpusHttp.body.data.summary}}

                        知识库内容：
                        {{knowledge.content}}

                        编研大纲参考：
                        {{outlineText}}

                        请撰写本章节的 Markdown 正文（以 ## 标题 开头，不少于200字）。"""
        );
        step.put("type", "LLM");
        return step;
    }

    public static String buildDefinitionJson(ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize research workflow definition.", exception);
        }
    }

    private static WorkflowNode node(
            String id,
            WorkflowNodeType type,
            String name,
            int x,
            int y,
            Map<String, Object> config
    ) {
        Map<String, Object> nodeConfig = new LinkedHashMap<>(config);
        nodeConfig.put("ui", Map.of("position", Map.of("x", x, "y", y)));
        return new WorkflowNode(id, type, name, nodeConfig);
    }

    private static WorkflowEdge edge(String id, String sourceNodeId, String targetNodeId) {
        return new WorkflowEdge(id, sourceNodeId, targetNodeId, null);
    }

    private static Map<String, String> param(String name, String source) {
        return Map.of("name", name, "value", source);
    }
}
