package com.aiworkflow.workflow.service;

import com.aiworkflow.workflow.domain.Workflow;
import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowEdge;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import com.aiworkflow.workflow.domain.WorkflowStatus;
import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.domain.WorkflowVersionStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowApplicationServiceTest {
    private final WorkflowStore store = new InMemoryWorkflowStore();
    private final WorkflowApplicationService service = new WorkflowApplicationService(store, new DagValidator());

    @Test
    void createsWorkflowWithDraftVersion() {
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                "Routes incoming messages",
                "user-1",
                validDefinition()
        );

        assertThat(workflow.id()).isNotBlank();
        assertThat(workflow.tenantId()).isEqualTo("tenant-1");
        assertThat(workflow.name()).isEqualTo("Support triage");
        assertThat(workflow.description()).isEqualTo("Routes incoming messages");
        assertThat(workflow.status()).isEqualTo(WorkflowStatus.DRAFT);
        assertThat(workflow.currentVersionId()).isNull();
        assertThat(workflow.createdBy()).isEqualTo("user-1");
        assertThat(workflow.createdAt()).isNotNull();
        assertThat(workflow.updatedAt()).isEqualTo(workflow.createdAt());

        List<WorkflowVersion> versions = service.listVersions(workflow.id());
        assertThat(versions).hasSize(1);
        WorkflowVersion draft = versions.get(0);
        assertThat(draft.workflowId()).isEqualTo(workflow.id());
        assertThat(draft.version()).isEqualTo(1);
        assertThat(draft.definition()).isEqualTo(validDefinition());
        assertThat(draft.status()).isEqualTo(WorkflowVersionStatus.DRAFT);
        assertThat(draft.publishedBy()).isNull();
        assertThat(draft.publishedAt()).isNull();
        assertThat(draft.createdAt()).isNotNull();
    }

    @Test
    void createsWorkflowDraftWithoutPublishValidation() {
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "user-1",
                definitionWithCycle()
        );

        assertThat(service.listVersions(workflow.id()).get(0).definition()).isEqualTo(definitionWithCycle());
    }

    @Test
    void rejectsInvalidDraftWhenPublishing() {
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "user-1",
                definitionWithCycle()
        );

        assertThatThrownBy(() -> service.publishDraftVersion(workflow.id(), "publisher-1"))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow definition must be acyclic.");

        assertThat(service.getWorkflow(workflow.id()).status()).isEqualTo(WorkflowStatus.DRAFT);
    }

    @Test
    void updatesIncompleteDraftDefinitionBeforePublishValidation() {
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "user-1",
                validDefinition()
        );
        WorkflowDefinition updatedDefinition = incompleteDefinition();

        WorkflowVersion updatedDraft = service.updateDraftDefinition(workflow.id(), updatedDefinition);

        assertThat(updatedDraft.definition()).isEqualTo(updatedDefinition);
        assertThat(updatedDraft.status()).isEqualTo(WorkflowVersionStatus.DRAFT);
    }

    @Test
    void rejectsStructurallyInvalidDraftUpdate() {
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "user-1",
                validDefinition()
        );

        assertThatThrownBy(() -> service.updateDraftDefinition(workflow.id(), definitionWithCycle()))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow definition must be acyclic.");

        assertThat(service.listVersions(workflow.id()).get(0).definition()).isEqualTo(validDefinition());
    }

    @Test
    void publishesDraftAsImmutableVersion() {
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "user-1",
                validDefinition()
        );

        WorkflowVersion publishedVersion = service.publishDraftVersion(workflow.id(), "publisher-1");

        assertThat(publishedVersion.status()).isEqualTo(WorkflowVersionStatus.PUBLISHED);
        assertThat(publishedVersion.publishedBy()).isEqualTo("publisher-1");
        assertThat(publishedVersion.publishedAt()).isNotNull();

        Workflow publishedWorkflow = service.getWorkflow(workflow.id());
        assertThat(publishedWorkflow.status()).isEqualTo(WorkflowStatus.PUBLISHED);
        assertThat(publishedWorkflow.currentVersionId()).isEqualTo(publishedVersion.id());
        assertThat(publishedWorkflow.updatedAt()).isNotEqualTo(workflow.updatedAt());

        assertThat(service.listWorkflows()).containsExactly(publishedWorkflow);
    }

    @Test
    void publishedVersionDefinitionIsNotMutatedByOriginalDefinitionCollections() {
        Map<String, Object> transformConfig = new HashMap<>();
        transformConfig.put("template", "original");
        List<WorkflowNode> nodes = new ArrayList<>(List.of(
                node("start", WorkflowNodeType.START),
                new WorkflowNode("transform", WorkflowNodeType.TEXT_TRANSFORM, "transform", transformConfig),
                node("end", WorkflowNodeType.END)
        ));
        WorkflowDefinition definition = new WorkflowDefinition(
                nodes,
                List.of(
                        edge("edge-1", "start", "transform"),
                        edge("edge-2", "transform", "end")
                ),
                List.of()
        );
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "user-1",
                definition
        );

        nodes.add(node("late-node", WorkflowNodeType.TEXT_TRANSFORM));
        transformConfig.put("template", "mutated");

        WorkflowVersion publishedVersion = service.publishDraftVersion(workflow.id(), "publisher-1");

        assertThat(publishedVersion.definition().nodes())
                .extracting(WorkflowNode::id)
                .containsExactly("start", "transform", "end");
        assertThat(publishedVersion.definition().nodes().get(1).config())
                .containsEntry("template", "original");
    }

    @Test
    void createsNewDraftWhenUpdatingAfterPublish() {
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "user-1",
                validDefinition()
        );
        service.publishDraftVersion(workflow.id(), "publisher-1");

        WorkflowVersion updatedDraft = service.updateDraftDefinition(workflow.id(), definitionWithExtraTransform());

        assertThat(updatedDraft.status()).isEqualTo(WorkflowVersionStatus.DRAFT);
        assertThat(updatedDraft.version()).isEqualTo(2);
        assertThat(updatedDraft.definition()).isEqualTo(definitionWithExtraTransform());
        assertThat(service.listVersions(workflow.id()))
                .extracting(WorkflowVersion::status)
                .containsExactly(WorkflowVersionStatus.PUBLISHED, WorkflowVersionStatus.DRAFT);
    }

    @Test
    void publishingTwiceWithoutDraftKeepsCurrentPublishedVersion() {
        Workflow workflow = service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "user-1",
                validDefinition()
        );
        WorkflowVersion firstPublish = service.publishDraftVersion(workflow.id(), "publisher-1");

        WorkflowVersion secondPublish = service.publishDraftVersion(workflow.id(), "publisher-2");

        assertThat(secondPublish).isEqualTo(firstPublish);
        assertThat(service.listVersions(workflow.id())).hasSize(1);
    }

    @Test
    void throwsWhenWorkflowDoesNotExist() {
        assertThatThrownBy(() -> service.getWorkflow("missing-workflow"))
                .isInstanceOf(WorkflowNotFoundException.class)
                .hasMessage("Workflow not found: missing-workflow");

        assertThatThrownBy(() -> service.updateDraftDefinition("missing-workflow", validDefinition()))
                .isInstanceOf(WorkflowNotFoundException.class)
                .hasMessage("Workflow not found: missing-workflow");

        assertThatThrownBy(() -> service.publishDraftVersion("missing-workflow", "publisher-1"))
                .isInstanceOf(WorkflowNotFoundException.class)
                .hasMessage("Workflow not found: missing-workflow");
    }

    private WorkflowDefinition validDefinition() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("transform", WorkflowNodeType.TEXT_TRANSFORM),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(
                        edge("edge-1", "start", "transform"),
                        edge("edge-2", "transform", "end")
                ),
                List.of()
        );
    }

    private WorkflowDefinition definitionWithExtraTransform() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("transform-1", WorkflowNodeType.TEXT_TRANSFORM),
                        node("transform-2", WorkflowNodeType.TEXT_TRANSFORM),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(
                        edge("edge-1", "start", "transform-1"),
                        edge("edge-2", "transform-1", "transform-2"),
                        edge("edge-3", "transform-2", "end")
                ),
                List.of()
        );
    }

    private WorkflowDefinition incompleteDefinition() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("transform", WorkflowNodeType.TEXT_TRANSFORM)
                ),
                List.of(edge("edge-1", "start", "transform")),
                List.of()
        );
    }

    private WorkflowDefinition definitionWithCycle() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("transform-1", WorkflowNodeType.TEXT_TRANSFORM),
                        node("transform-2", WorkflowNodeType.TEXT_TRANSFORM),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(
                        edge("edge-1", "start", "transform-1"),
                        edge("edge-2", "transform-1", "transform-2"),
                        edge("edge-3", "transform-2", "transform-1"),
                        edge("edge-4", "transform-2", "end")
                ),
                List.of()
        );
    }

    private WorkflowNode node(String id, WorkflowNodeType type) {
        return new WorkflowNode(id, type, id, Map.of());
    }

    private WorkflowEdge edge(String id, String sourceNodeId, String targetNodeId) {
        return new WorkflowEdge(id, sourceNodeId, targetNodeId, null);
    }
}
