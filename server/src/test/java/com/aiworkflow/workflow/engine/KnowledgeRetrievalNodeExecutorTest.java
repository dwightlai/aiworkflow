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
}
