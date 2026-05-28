package com.aiworkflow.workflow.service;

import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowEdge;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DagValidatorTest {
    private final DagValidator validator = new DagValidator();

    @Test
    void acceptsSingleStartAndEndConnectedDag() {
        WorkflowDefinition definition = validDefinition();

        assertThatCode(() -> validator.validate(definition))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDefinitionWithoutStartNode() {
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(
                        node("transform", WorkflowNodeType.TEXT_TRANSFORM),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(edge("edge-1", "transform", "end")),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow definition must contain exactly one START node.");
    }

    @Test
    void rejectsDefinitionWithCycle() {
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("transform-a", WorkflowNodeType.TEXT_TRANSFORM),
                        node("transform-b", WorkflowNodeType.TEXT_TRANSFORM),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(
                        edge("edge-1", "start", "transform-a"),
                        edge("edge-2", "transform-a", "transform-b"),
                        edge("edge-3", "transform-b", "transform-a"),
                        edge("edge-4", "transform-b", "end")
                ),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow definition must be acyclic.");
    }

    @Test
    void rejectsEdgeWithMissingTargetNode() {
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(edge("edge-1", "start", "missing")),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow edge references missing node.");
    }

    @Test
    void rejectsEdgeWithMissingSourceNode() {
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(edge("edge-1", "missing", "end")),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow edge references missing node.");
    }

    @Test
    void rejectsDuplicateNodeId() {
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("duplicate", WorkflowNodeType.TEXT_TRANSFORM),
                        node("duplicate", WorkflowNodeType.TEXT_TRANSFORM),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(
                        edge("edge-1", "start", "duplicate"),
                        edge("edge-2", "duplicate", "end")
                ),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow node ids must be unique.");
    }

    @Test
    void rejectsNullNodeElement() {
        WorkflowDefinition definition = new WorkflowDefinition(
                Arrays.asList(
                        node("start", WorkflowNodeType.START),
                        null,
                        node("end", WorkflowNodeType.END)
                ),
                List.of(edge("edge-1", "start", "end")),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow node id is required.");
    }

    @Test
    void rejectsBlankNodeId() {
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node(" ", WorkflowNodeType.TEXT_TRANSFORM),
                        node("end", WorkflowNodeType.END)
                ),
                List.of(
                        edge("edge-1", "start", " "),
                        edge("edge-2", " ", "end")
                ),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow node id is required.");
    }

    @Test
    void rejectsNullEdgeElement() {
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START),
                        node("end", WorkflowNodeType.END)
                ),
                Collections.singletonList(null),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(DagValidationException.class)
                .hasMessage("Workflow edge references missing node.");
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

    private WorkflowNode node(String id, WorkflowNodeType type) {
        return new WorkflowNode(id, type, id, Map.of());
    }

    private WorkflowEdge edge(String id, String sourceNodeId, String targetNodeId) {
        return new WorkflowEdge(id, sourceNodeId, targetNodeId, null);
    }
}
