package com.aiworkflow.persistence;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeChunkVector;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import com.aiworkflow.knowledge.domain.VectorStoreConfig;
import com.aiworkflow.knowledge.service.KnowledgeStore;
import com.aiworkflow.knowledge.service.VectorStoreConfigStore;
import com.aiworkflow.model.domain.ModelProvider;
import com.aiworkflow.model.service.ModelProviderStore;
import com.aiworkflow.prompt.domain.PromptTemplate;
import com.aiworkflow.prompt.service.PromptTemplateStore;
import com.aiworkflow.workflow.domain.Workflow;
import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import com.aiworkflow.workflow.domain.WorkflowStatus;
import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.domain.WorkflowVersionStatus;
import com.aiworkflow.workflow.engine.NodeExecution;
import com.aiworkflow.workflow.engine.NodeExecutionStatus;
import com.aiworkflow.workflow.engine.WorkflowExecution;
import com.aiworkflow.workflow.engine.WorkflowExecutionStatus;
import com.aiworkflow.workflow.engine.WorkflowExecutionStore;
import com.aiworkflow.workflow.service.WorkflowStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mybatis-persistence;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false"
})
class MybatisPersistenceIntegrationTest {
    @Autowired
    private ModelProviderStore modelProviderStore;

    @Autowired
    private PromptTemplateStore promptTemplateStore;

    @Autowired
    private VectorStoreConfigStore vectorStoreConfigStore;

    @Autowired
    private KnowledgeStore knowledgeStore;

    @Autowired
    private WorkflowStore workflowStore;

    @Autowired
    private WorkflowExecutionStore workflowExecutionStore;

    @Test
    void persistsModelProvidersAndPromptTemplatesWithMybatisPlus() {
        Instant now = Instant.parse("2026-06-03T08:00:00Z");

        modelProviderStore.save(new ModelProvider(
                "mp_1",
                "DeepSeek Chat",
                "DeepSeek",
                "chat",
                "chat model",
                false,
                new BigDecimal("0.80"),
                "https://api.deepseek.com/v1",
                "deepseek-chat",
                "secret-ref",
                true,
                now,
                now
        ));
        promptTemplateStore.save(new PromptTemplate("pt_1", "客服问候", "你好，{{name}}", "greeting", now, now));

        assertThat(modelProviderStore.findById("mp_1")).isPresent();
        assertThat(promptTemplateStore.findById("pt_1")).isPresent();
    }

    @Test
    void persistsKnowledgeDocumentsChunksVectorsAndVectorStoreConfigWithMybatisPlus() {
        Instant now = Instant.parse("2026-06-03T08:10:00Z");

        vectorStoreConfigStore.save(new VectorStoreConfig(
                "vs_1",
                "Elasticsearch",
                "ELASTICSEARCH",
                "http://localhost:9200",
                "agi_kb",
                "elastic",
                "password",
                null,
                3000,
                10000,
                true,
                now,
                now
        ));
        knowledgeStore.saveKnowledgeBase(new KnowledgeBase(
                "kb_1",
                "客服手册",
                "FAQ",
                "mp_embed",
                "vs_1",
                1024,
                "fixed",
                500,
                50,
                "vector",
                5,
                "READY",
                1,
                1,
                now,
                now
        ));
        knowledgeStore.saveDocument(new KnowledgeDocument("doc_1", "kb_1", "faq.md", 1, now));
        knowledgeStore.saveChunk(new KnowledgeChunk("chunk_1", "kb_1", "doc_1", "faq.md", "怎么退款", 0, true, 12));
        knowledgeStore.saveChunkVector(new KnowledgeChunkVector("chunk_1", "kb_1", "doc_1", "mp_embed", List.of(0.1, 0.2), now));

        assertThat(vectorStoreConfigStore.findById("vs_1")).isPresent();
        assertThat(knowledgeStore.findKnowledgeBaseById("kb_1")).isPresent();
        assertThat(knowledgeStore.listDocuments("kb_1")).hasSize(1);
        assertThat(knowledgeStore.listChunks("kb_1")).extracting(KnowledgeChunk::content).containsExactly("怎么退款");
        assertThat(knowledgeStore.listChunkVectors("kb_1")).singleElement()
                .satisfies(vector -> assertThat(vector.embedding()).containsExactly(0.1, 0.2));
    }

    @Test
    void persistsWorkflowVersionsAndExecutionHistoryWithMybatisPlus() {
        Instant now = Instant.parse("2026-06-03T08:20:00Z");
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(new WorkflowNode("start", WorkflowNodeType.START, "开始", Map.of("input", "question"))),
                List.of(),
                List.of()
        );

        workflowStore.saveWorkflow(new Workflow(
                "wf_1",
                "tenant-default",
                "测试工作流",
                "Smoke",
                WorkflowStatus.DRAFT,
                null,
                "tester",
                now,
                now
        ));
        workflowStore.saveVersion(new WorkflowVersion(
                "wv_1",
                "wf_1",
                1,
                definition,
                WorkflowVersionStatus.DRAFT,
                null,
                null,
                now
        ));
        workflowStore.saveWorkflow(new Workflow(
                "wf_1",
                "tenant-default",
                "测试工作流",
                "Smoke",
                WorkflowStatus.DRAFT,
                "wv_1",
                "tester",
                now,
                now
        ));
        workflowExecutionStore.saveWorkflowExecution(new WorkflowExecution(
                "run_1",
                "wf_1",
                "wv_1",
                WorkflowExecutionStatus.SUCCEEDED,
                Map.of("question", "hello"),
                Map.of("answer", "world"),
                null,
                now,
                now.plusSeconds(1)
        ));
        workflowExecutionStore.saveNodeExecution(new NodeExecution(
                "node_run_1",
                "run_1",
                "start",
                WorkflowNodeType.START,
                NodeExecutionStatus.SUCCEEDED,
                Map.of("question", "hello"),
                Map.of("question", "hello"),
                null,
                now,
                now
        ));

        assertThat(workflowStore.findWorkflowById("wf_1")).isPresent();
        assertThat(workflowStore.findVersionById("wv_1")).isPresent();
        assertThat(workflowExecutionStore.findWorkflowExecutionById("run_1")).isPresent();
        assertThat(workflowExecutionStore.listNodeExecutions("run_1")).hasSize(1);
    }
}
