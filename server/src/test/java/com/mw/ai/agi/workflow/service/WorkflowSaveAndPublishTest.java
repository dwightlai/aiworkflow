package com.mw.ai.agi.workflow.service;

import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowEdge;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.mw.ai.agi.workflow.domain.WorkflowStatus;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;
import com.mw.ai.agi.workflow.domain.WorkflowVersionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowSaveAndPublishTest {
    private final WorkflowStore store = new InMemoryWorkflowStore();
    private final WorkflowApplicationService service = new WorkflowApplicationService(store, new DagValidator());

    @BeforeEach
    void setTenant() {
        TenantContext.set("tenant-1");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void savesAndPublishesExactMetadataAndDefinition() {
        Workflow workflow = createWorkflow();
        WorkflowDefinition definition = definitionWithKnowledgeBases();

        WorkflowVersion published = service.saveAndPublish(
                workflow.id(),
                "Knowledge federation",
                "Searches two knowledge bases",
                definition,
                "publisher-1"
        );

        Workflow updated = service.getWorkflow(workflow.id());
        assertThat(updated.name()).isEqualTo("Knowledge federation");
        assertThat(updated.description()).isEqualTo("Searches two knowledge bases");
        assertThat(updated.status()).isEqualTo(WorkflowStatus.PUBLISHED);
        assertThat(updated.currentVersionId()).isEqualTo(published.id());
        assertThat(published.status()).isEqualTo(WorkflowVersionStatus.PUBLISHED);
        assertThat(published.definition()).isEqualTo(definition);
        assertThat(published.definition().nodes().get(1).config().get("knowledgeBaseIds"))
                .isEqualTo(List.of("kb-standards", "kb-policies"));
    }

    @Test
    void validatesBeforeChangingMetadataDraftOrStatus() {
        Workflow workflow = createWorkflow();
        WorkflowVersion originalDraft = service.listVersions(workflow.id()).get(0);

        assertThatThrownBy(() -> service.saveAndPublish(
                workflow.id(),
                "Must not be saved",
                "Invalid snapshot",
                invalidDefinition(),
                "publisher-1"
        )).isInstanceOf(DagValidationException.class);

        Workflow unchanged = service.getWorkflow(workflow.id());
        WorkflowVersion unchangedDraft = service.listVersions(workflow.id()).get(0);
        assertThat(unchanged.name()).isEqualTo("Support triage");
        assertThat(unchanged.description()).isNull();
        assertThat(unchanged.status()).isEqualTo(WorkflowStatus.DRAFT);
        assertThat(unchanged.currentVersionId()).isNull();
        assertThat(unchangedDraft).isEqualTo(originalDraft);
    }

    @Test
    void rejectsArchivedWorkflowWithoutChangingMetadataOrDraft() {
        Workflow workflow = createWorkflow();
        Workflow archived = service.archiveWorkflow(workflow.id());
        WorkflowVersion originalDraft = service.listVersions(workflow.id()).get(0);

        assertThatThrownBy(() -> service.saveAndPublish(
                workflow.id(),
                "Must not be saved",
                "Archived snapshot",
                validDefinition(),
                "publisher-1"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Archived workflow cannot be published.");

        Workflow unchanged = service.getWorkflow(workflow.id());
        assertThat(unchanged).isEqualTo(archived);
        assertThat(service.listVersions(workflow.id())).containsExactly(originalDraft);
    }

    private Workflow createWorkflow() {
        return service.createWorkflow(
                "tenant-1",
                "Support triage",
                null,
                "creator-1",
                validDefinition()
        );
    }

    private WorkflowDefinition validDefinition() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, Map.of()),
                        node("end", WorkflowNodeType.END, Map.of())
                ),
                List.of(edge("edge-1", "start", "end")),
                List.of()
        );
    }

    private WorkflowDefinition definitionWithKnowledgeBases() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, Map.of()),
                        node(
                                "knowledge",
                                WorkflowNodeType.KNOWLEDGE_RETRIEVAL,
                                Map.of("knowledgeBaseIds", List.of("kb-standards", "kb-policies"))
                        ),
                        node("end", WorkflowNodeType.END, Map.of())
                ),
                List.of(
                        edge("edge-1", "start", "knowledge"),
                        edge("edge-2", "knowledge", "end")
                ),
                List.of()
        );
    }

    private WorkflowDefinition invalidDefinition() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, Map.of()),
                        node("end", WorkflowNodeType.END, Map.of())
                ),
                List.of(edge("broken", "start", "missing")),
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
