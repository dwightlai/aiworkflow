package com.aiworkflow.workflow.engine;

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

    private WorkflowNode node(String id, WorkflowNodeType type, Map<String, Object> config) {
        return new WorkflowNode(id, type, id, config);
    }

    private WorkflowEdge edge(String id, String sourceNodeId, String targetNodeId) {
        return new WorkflowEdge(id, sourceNodeId, targetNodeId, null);
    }
}
