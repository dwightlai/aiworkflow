package com.aiworkflow.workflow.engine;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.service.KnowledgeBaseService;
import com.aiworkflow.model.service.ModelProviderService;
import com.aiworkflow.workflow.domain.Workflow;
import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowEdge;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import com.aiworkflow.workflow.service.DagValidator;
import com.aiworkflow.workflow.service.InMemoryWorkflowStore;
import com.aiworkflow.workflow.service.WorkflowApplicationService;
import com.aiworkflow.workflow.service.WorkflowNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowExecutionServiceTest {
    private final WorkflowApplicationService workflowService = new WorkflowApplicationService(
            new InMemoryWorkflowStore(),
            new DagValidator()
    );
    private final WorkflowExecutionStore executionStore = new InMemoryWorkflowExecutionStore();
    private final WorkflowExecutionService executionService = new WorkflowExecutionService(
            workflowService,
            executionStore,
            new WorkflowNodeExecutorRegistry(List.of(
                    new StartNodeExecutor(),
                    new EndNodeExecutor(),
                    new TextTransformNodeExecutor(),
                    new ConditionNodeExecutor()
            ))
    );

    @Test
    void runsPublishedWorkflowFromStartToEnd() {
        Workflow workflow = createAndPublishWorkflow(linearDefinition());

        WorkflowExecutionResult result = executionService.runWorkflow(new WorkflowExecutionRequest(
                workflow.id(),
                Map.of("name", "Ada")
        ));

        assertThat(result.execution().status()).isEqualTo(WorkflowExecutionStatus.SUCCEEDED);
        assertThat(result.execution().output()).containsExactlyEntriesOf(Map.of("message", "Hello Ada"));
        assertThat(result.nodeExecutions())
                .extracting(NodeExecution::nodeId)
                .containsExactly("start", "transform", "end");
        assertThat(result.nodeExecutions())
                .extracting(NodeExecution::status)
                .containsOnly(NodeExecutionStatus.SUCCEEDED);
    }

    @Test
    void failsWhenWorkflowHasNoPublishedVersion() {
        Workflow workflow = workflowService.createWorkflow(
                "tenant-1",
                "Draft workflow",
                null,
                "user-1",
                linearDefinition()
        );

        assertThatThrownBy(() -> executionService.runWorkflow(new WorkflowExecutionRequest(workflow.id(), Map.of())))
                .isInstanceOf(WorkflowNotFoundException.class)
                .hasMessage("Published version not found for workflow: " + workflow.id());
    }

    @Test
    void conditionNodeChoosesMatchingBranch() {
        Workflow workflow = createAndPublishWorkflow(conditionDefinition());

        WorkflowExecutionResult result = executionService.runWorkflow(new WorkflowExecutionRequest(
                workflow.id(),
                Map.of("tier", "vip")
        ));

        assertThat(result.execution().status()).isEqualTo(WorkflowExecutionStatus.SUCCEEDED);
        assertThat(result.execution().output()).containsExactlyEntriesOf(Map.of("message", "VIP path"));
        assertThat(result.nodeExecutions())
                .extracting(NodeExecution::nodeId)
                .containsExactly("start", "condition", "vip-transform", "end");
    }

    @Test
    void recordsFailedNodeAndWorkflowWhenExecutorFails() {
        Workflow workflow = createAndPublishWorkflow(definitionWithBadTransformConfig());

        WorkflowExecutionResult result = executionService.runWorkflow(new WorkflowExecutionRequest(
                workflow.id(),
                Map.of("name", "Ada")
        ));

        assertThat(result.execution().status()).isEqualTo(WorkflowExecutionStatus.FAILED);
        assertThat(result.execution().errorMessage()).isEqualTo("Workflow node transform requires config: outputKey");
        assertThat(result.nodeExecutions())
                .extracting(NodeExecution::nodeId)
                .containsExactly("start", "transform");
        assertThat(result.nodeExecutions().getLast().status()).isEqualTo(NodeExecutionStatus.FAILED);
        assertThat(result.nodeExecutions().getLast().errorMessage())
                .isEqualTo("Workflow node transform requires config: outputKey");
    }

    @Test
    void runsAiflowyStyleKnowledgeAndLlmNodesThroughDag() {
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseService();
        KnowledgeBase knowledgeBase = knowledgeBaseService.create("客服手册", "客服问答资料");
        knowledgeBaseService.addDocument(knowledgeBase.id(), "refund.txt", "退款申请需要在七日内提交，客服会审核订单状态。");
        ModelProviderService modelProviderService = new ModelProviderService();
        String providerId = modelProviderService.create(
                "火山引擎",
                "VolcEngine",
                "CHAT",
                null,
                false,
                BigDecimal.ONE,
                "https://ark.cn-beijing.volces.com/api/v3",
                "DS-V3",
                "dev-key",
                true
        ).id();
        WorkflowExecutionService aiExecutionService = new WorkflowExecutionService(
                workflowService,
                executionStore,
                new WorkflowNodeExecutorRegistry(List.of(
                        new StartNodeExecutor(),
                        new KnowledgeRetrievalNodeExecutor(knowledgeBaseService),
                        new LlmNodeExecutor((provider, model, prompt, options) -> "llm:" + prompt, modelProviderService),
                        new EndNodeExecutor()
                ))
        );
        Workflow workflow = createAndPublishWorkflow(aiflowyStyleAiDefinition(knowledgeBase.id(), providerId));

        WorkflowExecutionResult result = aiExecutionService.runWorkflow(new WorkflowExecutionRequest(
                workflow.id(),
                Map.of("question", "如何退款", "keyword", "退款")
        ));

        assertThat(result.execution().status()).isEqualTo(WorkflowExecutionStatus.SUCCEEDED);
        assertThat(result.execution().output().get("output").toString()).contains("llm:你是客服助手");
        assertThat(result.execution().output().get("output").toString()).contains("问题：如何退款");
        assertThat(result.execution().output().get("output").toString()).contains("退款申请需要在七日内提交");
        assertThat(result.nodeExecutions())
                .extracting(NodeExecution::nodeId)
                .containsExactly("start", "knowledge", "llm", "end");
    }

    private Workflow createAndPublishWorkflow(WorkflowDefinition definition) {
        Workflow workflow = workflowService.createWorkflow(
                "tenant-1",
                "Executable workflow",
                null,
                "user-1",
                definition
        );
        workflowService.publishDraftVersion(workflow.id(), "publisher-1");
        return workflowService.getWorkflow(workflow.id());
    }

    private WorkflowDefinition linearDefinition() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, Map.of()),
                        node("transform", WorkflowNodeType.TEXT_TRANSFORM, Map.of(
                                "outputKey", "message",
                                "template", "Hello {{name}}"
                        )),
                        node("end", WorkflowNodeType.END, Map.of("outputKeys", List.of("message")))
                ),
                List.of(
                        edge("edge-1", "start", "transform"),
                        edge("edge-2", "transform", "end")
                ),
                List.of()
        );
    }

    private WorkflowDefinition conditionDefinition() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, Map.of()),
                        node("condition", WorkflowNodeType.CONDITION, Map.of(
                                "contextKey", "tier",
                                "equals", "vip",
                                "trueTargetNodeId", "vip-transform",
                                "falseTargetNodeId", "normal-transform"
                        )),
                        node("vip-transform", WorkflowNodeType.TEXT_TRANSFORM, Map.of(
                                "outputKey", "message",
                                "template", "VIP path"
                        )),
                        node("normal-transform", WorkflowNodeType.TEXT_TRANSFORM, Map.of(
                                "outputKey", "message",
                                "template", "Normal path"
                        )),
                        node("end", WorkflowNodeType.END, Map.of("outputKeys", List.of("message")))
                ),
                List.of(
                        edge("edge-1", "start", "condition"),
                        edge("edge-2", "condition", "vip-transform"),
                        edge("edge-3", "condition", "normal-transform"),
                        edge("edge-4", "vip-transform", "end"),
                        edge("edge-5", "normal-transform", "end")
                ),
                List.of()
        );
    }

    private WorkflowDefinition definitionWithBadTransformConfig() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, Map.of()),
                        node("transform", WorkflowNodeType.TEXT_TRANSFORM, Map.of("template", "Hello {{name}}")),
                        node("end", WorkflowNodeType.END, Map.of())
                ),
                List.of(
                        edge("edge-1", "start", "transform"),
                        edge("edge-2", "transform", "end")
                ),
                List.of()
        );
    }

    private WorkflowDefinition aiflowyStyleAiDefinition(String knowledgeBaseId, String providerId) {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, Map.of()),
                        node("knowledge", WorkflowNodeType.KNOWLEDGE_RETRIEVAL, Map.of(
                                "knowledgeBaseId", knowledgeBaseId,
                                "inputParams", List.of(Map.of("name", "search_key", "value", "keyword", "type", "String")),
                                "keywordTemplate", "{{search_key}}",
                                "fetchCount", 5,
                                "outputParams", List.of(
                                        Map.of("name", "documents", "type", "Array"),
                                        Map.of("name", "title", "type", "String"),
                                        Map.of("name", "content", "type", "String"),
                                        Map.of("name", "documentId", "type", "Number"),
                                        Map.of("name", "knowledgeId", "type", "Number")
                                )
                        )),
                        node("llm", WorkflowNodeType.LLM, Map.of(
                                "providerId", providerId,
                                "inputParams", List.of(Map.of("name", "question", "value", "question", "type", "String")),
                                "systemPrompt", "你是客服助手",
                                "userPrompt", "问题：{{question}}\n知识：{{documents}}",
                                "temperature", 0.5,
                                "topP", 0.9,
                                "topK", 50,
                                "outputParams", List.of(Map.of("name", "output", "type", "String"))
                        )),
                        node("end", WorkflowNodeType.END, Map.of("outputKeys", List.of("output")))
                ),
                List.of(
                        edge("edge-1", "start", "knowledge"),
                        edge("edge-2", "knowledge", "llm"),
                        edge("edge-3", "llm", "end")
                ),
                List.of()
        );
    }

    private WorkflowNode node(String id, WorkflowNodeType type, Map<String, Object> config) {
        return new WorkflowNode(id, type, id, config);
    }

    private WorkflowEdge edge(String id, String sourceNodeId, String targetNodeId) {
        return new WorkflowEdge(id, sourceNodeId, targetNodeId, null);
    }
}
