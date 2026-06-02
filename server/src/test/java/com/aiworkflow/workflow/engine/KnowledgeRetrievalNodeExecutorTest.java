package com.aiworkflow.workflow.engine;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.service.KnowledgeBaseService;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRetrievalNodeExecutorTest {

    @Test
    void searchesConfiguredKnowledgeBaseAndWritesResultsToContextKey() {
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseService();
        KnowledgeBase knowledgeBase = knowledgeBaseService.create("产品知识库", "客服问答资料");
        knowledgeBaseService.addDocument(knowledgeBase.id(), "faq.txt", "发票可以在订单完成后七日内申请。退货需要保留包装。");
        KnowledgeRetrievalNodeExecutor executor = new KnowledgeRetrievalNodeExecutor(knowledgeBaseService);

        NodeExecutionResult result = executor.execute(
                new WorkflowNode(
                        "knowledge_1",
                        WorkflowNodeType.KNOWLEDGE_RETRIEVAL,
                        "知识库检索",
                        Map.of(
                                "knowledgeBaseId", knowledgeBase.id(),
                                "queryKey", "question",
                                "outputKey", "contexts",
                                "topK", 2
                        )
                ),
                new NodeExecutionContext(Map.of(), Map.of("question", "如何申请发票"))
        );

        assertThat(result.output()).containsKey("contexts");
        assertThat((List<?>) result.output().get("contexts")).hasSize(1);
        assertThat(result.output().get("contexts").toString()).contains("发票可以在订单完成后七日内申请");
    }

    @Test
    void searchesWithAiflowyStyleInputKeywordAndOutputParams() {
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseService();
        KnowledgeBase knowledgeBase = knowledgeBaseService.create("客服手册", "客服问答资料");
        knowledgeBaseService.addDocument(knowledgeBase.id(), "refund.txt", "退款申请需要在七日内提交，客服会审核订单状态。");
        KnowledgeRetrievalNodeExecutor executor = new KnowledgeRetrievalNodeExecutor(knowledgeBaseService);

        NodeExecutionResult result = executor.execute(
                new WorkflowNode(
                        "knowledge_1",
                        WorkflowNodeType.KNOWLEDGE_RETRIEVAL,
                        "知识库",
                        Map.of(
                                "knowledgeBaseId", knowledgeBase.id(),
                                "inputParams", List.of(Map.of("name", "search_key", "value", "keyword", "type", "String")),
                                "keywordTemplate", "{{search_key}}",
                                "fetchCount", 5,
                                "outputParams", List.of(Map.of("name", "documents", "type", "Array"))
                        )
                ),
                new NodeExecutionContext(Map.of(), Map.of("keyword", "退款"))
        );

        assertThat(result.output()).containsKey("documents");
        assertThat((List<?>) result.output().get("documents")).hasSize(1);
        assertThat(result.output().get("documents").toString()).contains("退款申请需要在七日内提交");
    }
}
