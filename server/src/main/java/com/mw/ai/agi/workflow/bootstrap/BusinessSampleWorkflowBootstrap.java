package com.mw.ai.agi.workflow.bootstrap;

import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.service.KnowledgeBaseService;
import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.service.ModelProviderService;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowEdge;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class BusinessSampleWorkflowBootstrap {
    private static final Logger log = LoggerFactory.getLogger(BusinessSampleWorkflowBootstrap.class);
    private static final String TENANT_ID = "tenant_default";
    private static final String CREATED_BY = "system";
    private static final String CUSTOMER_SERVICE_NAME = "客服问答助手";
    private static final String CONTRACT_EXTRACTION_NAME = "合同条款抽取";
    private static final String SAMPLE_KB_NAME = "客服问答样例库";

    @Bean
    ApplicationRunner seedBusinessSampleWorkflows(
            WorkflowApplicationService workflowService,
            KnowledgeBaseService knowledgeBaseService,
            ModelProviderService modelProviderService
    ) {
        return args -> {
            TenantContext.set(TenantContext.normalize(TENANT_ID));
            try {
                Optional<ModelProvider> chatProvider = resolveChatProvider(modelProviderService);
                if (chatProvider.isEmpty()) {
                    log.warn("Skip seeding business sample workflows: no enabled CHAT model provider");
                    return;
                }
                String providerId = chatProvider.get().id();
                String model = chatProvider.get().model();
                seedCustomerServiceWorkflow(workflowService, knowledgeBaseService, providerId, model);
                seedContractExtractionWorkflow(workflowService, providerId, model);
            } finally {
                TenantContext.clear();
            }
        };
    }

    private void seedCustomerServiceWorkflow(
            WorkflowApplicationService workflowService,
            KnowledgeBaseService knowledgeBaseService,
            String providerId,
            String model
    ) {
        String knowledgeBaseId = ensureCustomerServiceKnowledgeBase(knowledgeBaseService);
        upsertSampleWorkflow(
                workflowService,
                CUSTOMER_SERVICE_NAME,
                "客服问答样例：检索知识库并结合大模型生成回答",
                customerServiceDefinition(knowledgeBaseId, providerId, model)
        );
    }

    private void seedContractExtractionWorkflow(
            WorkflowApplicationService workflowService,
            String providerId,
            String model
    ) {
        upsertSampleWorkflow(
                workflowService,
                CONTRACT_EXTRACTION_NAME,
                "合同条款抽取样例：从合同正文中结构化提取关键条款",
                contractExtractionDefinition(providerId, model)
        );
    }

    private void upsertSampleWorkflow(
            WorkflowApplicationService workflowService,
            String name,
            String description,
            WorkflowDefinition definition
    ) {
        var existing = workflowService.listWorkflows().stream()
                .filter(workflow -> name.equals(workflow.name()))
                .findFirst();
        if (existing.isPresent()) {
            workflowService.updateDraftDefinition(existing.get().id(), definition);
            workflowService.publishDraftVersion(existing.get().id(), CREATED_BY);
            return;
        }
        Workflow workflow = workflowService.createWorkflow(
                TENANT_ID,
                name,
                description,
                CREATED_BY,
                definition
        );
        workflowService.publishDraftVersion(workflow.id(), CREATED_BY);
    }

    private String ensureCustomerServiceKnowledgeBase(KnowledgeBaseService knowledgeBaseService) {
        Optional<KnowledgeBase> existing = knowledgeBaseService.list().stream()
                .filter(base -> SAMPLE_KB_NAME.equals(base.name()))
                .findFirst();
        if (existing.isPresent()) {
            ensureSampleDocuments(knowledgeBaseService, existing.get().id());
            return existing.get().id();
        }
        KnowledgeBase created = knowledgeBaseService.create(SAMPLE_KB_NAME, "客服问答助手样例知识库");
        ensureSampleDocuments(knowledgeBaseService, created.id());
        return created.id();
    }

    private void ensureSampleDocuments(KnowledgeBaseService knowledgeBaseService, String knowledgeBaseId) {
        if (!knowledgeBaseService.listDocuments(knowledgeBaseId).isEmpty()) {
            return;
        }
        knowledgeBaseService.addDocument(
                knowledgeBaseId,
                "退款政策.txt",
                "退款申请需在订单签收后7日内提交，客服审核通过后3-5个工作日原路退回。部分定制商品不支持无理由退款。"
        );
        knowledgeBaseService.addDocument(
                knowledgeBaseId,
                "配送时效.txt",
                "标准配送一般3-5个工作日送达，偏远地区可能延长2-3天。支持物流轨迹查询。"
        );
        knowledgeBaseService.addDocument(
                knowledgeBaseId,
                "会员权益.txt",
                "会员用户享受专属客服通道、生日礼券及积分加倍权益。会员等级越高，退换货处理优先级越高。"
        );
    }

    static WorkflowDefinition customerServiceDefinition(String knowledgeBaseId, String providerId, String model) {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, "开始", 32, 120, Map.of(
                                "inputParams", List.of(
                                        Map.of("name", "question", "type", "String", "required", true),
                                        Map.of("name", "keyword", "type", "String", "required", false)
                                ),
                                "defaultInputJson", "{\"question\":\"如何申请退款？\",\"keyword\":\"退款\"}"
                        )),
                        node("knowledge", WorkflowNodeType.KNOWLEDGE_RETRIEVAL, "知识检索", 292, 120, Map.of(
                                "knowledgeBaseId", knowledgeBaseId,
                                "knowledgeBaseIds", List.of(knowledgeBaseId),
                                "inputParams", List.of(Map.of("name", "question", "value", "question", "type", "String")),
                                "queryText", "{{question}}",
                                "fetchCount", 5,
                                "topK", 5,
                                "similarityThreshold", 0,
                                "outputKey", "content",
                                "outputFormat", "TEXT",
                                "outputParams", List.of(
                                        Map.of("name", "content", "type", "String"),
                                        Map.of("name", "sources", "type", "Array"),
                                        Map.of("name", "query", "type", "String")
                                )
                        )),
                        node("llm", WorkflowNodeType.LLM, "客服回答", 552, 120, Map.of(
                                "providerId", providerId,
                                "model", model,
                                "inputParams", List.of(Map.of("name", "question", "value", "question", "type", "String")),
                                "systemPrompt", "你是专业客服助手，请基于提供的知识内容准确、礼貌地回答用户问题。若知识中没有相关信息，请明确说明并建议联系人工客服。",
                                "userPrompt", "用户问题：{{question}}\n\n参考知识：\n{{content}}\n\n请给出简洁专业的回答。",
                                "outputKey", "answer",
                                "temperature", 0.3,
                                "topP", 0.9,
                                "topK", 40,
                                "outputParams", List.of(Map.of("name", "answer", "type", "String"))
                        )),
                        node("end", WorkflowNodeType.END, "结束", 812, 120, Map.of(
                                "outputParams", List.of(
                                        Map.of("name", "question", "value", "question", "type", "String"),
                                        Map.of("name", "answer", "value", "answer", "type", "String"),
                                        Map.of("name", "knowledge", "value", "content", "type", "String")
                                )
                        ))
                ),
                List.of(
                        edge("edge-1", "start", "knowledge", null),
                        edge("edge-2", "knowledge", "llm", null),
                        edge("edge-3", "llm", "end", null)
                ),
                List.of()
        );
    }

    static WorkflowDefinition contractExtractionDefinition(String providerId, String model) {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, "开始", 32, 120, Map.of(
                                "inputParams", List.of(Map.of("name", "contractText", "type", "String", "required", true)),
                                "defaultInputJson", """
                                        {"contractText":"甲乙方就软件服务达成如下约定：\\n1. 服务期限：自2024年1月1日起至2024年12月31日止。\\n2. 服务费用：人民币10万元，签约后7日内支付。\\n3. 保密义务：双方对合作中知悉的商业秘密负有保密义务，期限为合作终止后3年。\\n4. 违约责任：任何一方违约应向对方支付合同总额20%的违约金。\\n5. 争议解决：协商不成提交北京仲裁委员会仲裁。"}
                                        """.trim()
                        )),
                        node("llm", WorkflowNodeType.LLM, "条款抽取", 292, 120, Map.of(
                                "providerId", providerId,
                                "model", model,
                                "inputParams", List.of(Map.of("name", "contractText", "value", "contractText", "type", "String")),
                                "systemPrompt", "你是合同审查助手。请从合同正文中抽取关键条款，输出 JSON 数组，每项包含 type（条款类型）、summary（摘要）、originalText（原文摘录）。只输出 JSON，不要附加说明。",
                                "userPrompt", "请抽取以下合同的关键条款：\n\n{{contractText}}",
                                "outputKey", "clauses",
                                "temperature", 0.2,
                                "topP", 0.9,
                                "topK", 40,
                                "outputParams", List.of(Map.of("name", "clauses", "type", "String"))
                        )),
                        node("end", WorkflowNodeType.END, "结束", 552, 120, Map.of(
                                "outputParams", List.of(
                                        Map.of("name", "clauses", "value", "clauses", "type", "String")
                                )
                        ))
                ),
                List.of(
                        edge("edge-1", "start", "llm", null),
                        edge("edge-2", "llm", "end", null)
                ),
                List.of()
        );
    }

    private Optional<ModelProvider> resolveChatProvider(ModelProviderService modelProviderService) {
        return modelProviderService.list().stream()
                .filter(provider -> provider.enabled() && "CHAT".equalsIgnoreCase(provider.modelUsage()))
                .findFirst();
    }

    private static WorkflowNode node(
            String id,
            WorkflowNodeType type,
            String name,
            int x,
            int y,
            Map<String, Object> config
    ) {
        Map<String, Object> merged = new LinkedHashMap<>(config);
        merged.put("ui", Map.of("position", Map.of("x", x, "y", y)));
        return new WorkflowNode(id, type, name, merged);
    }

    private static WorkflowEdge edge(String id, String sourceNodeId, String targetNodeId, String condition) {
        return new WorkflowEdge(id, sourceNodeId, targetNodeId, condition);
    }
}
