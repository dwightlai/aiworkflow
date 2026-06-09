package com.mw.ai.agi.workflow.bootstrap;

import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowEdge;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class QuestionClassifierSampleWorkflowBootstrap {
    private static final String SAMPLE_NAME = "问题分类测试";
    private static final String TENANT_ID = "tenant_default";
    private static final String CREATED_BY = "system";

    @Bean
    ApplicationRunner seedQuestionClassifierSampleWorkflow(WorkflowApplicationService workflowService) {
        return args -> {
            if (workflowService.listWorkflows().stream().anyMatch(workflow -> SAMPLE_NAME.equals(workflow.name()))) {
                return;
            }
            Workflow workflow = workflowService.createWorkflow(
                    TENANT_ID,
                    SAMPLE_NAME,
                    "问题分类节点示例：按关键词路由到不同分支",
                    CREATED_BY,
                    definition()
            );
            workflowService.publishDraftVersion(workflow.id(), CREATED_BY);
        };
    }

    static WorkflowDefinition definition() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, "开始", 32, 120, Map.of(
                                "inputParams", List.of(Map.of("name", "message", "type", "String", "required", true)),
                                "defaultInputJson", "{\"message\":\"我要申请退款\"}"
                        )),
                        node("classifier", WorkflowNodeType.QUESTION_CLASSIFIER, "问题分类", 262, 120, Map.of(
                                "contentTemplate", "${message}",
                                "outputKey", "index",
                                "categories", List.of(
                                        Map.of(
                                                "id", "分类1",
                                                "name", "售后咨询",
                                                "keywords", List.of("退款", "退货"),
                                                "matchMode", "CONTAINS"
                                        ),
                                        Map.of(
                                                "id", "分类2",
                                                "name", "其他问题",
                                                "keywords", List.of("天气"),
                                                "matchMode", "CONTAINS"
                                        )
                                ),
                                "outputParams", List.of(Map.of("name", "index", "type", "String"))
                        )),
                        node("after-sales", WorkflowNodeType.TEXT_TRANSFORM, "售后分支", 492, 60, Map.of(
                                "outputKey", "result",
                                "template", "售后分支"
                        )),
                        node("other", WorkflowNodeType.TEXT_TRANSFORM, "其他分支", 492, 180, Map.of(
                                "outputKey", "result",
                                "template", "其他分支"
                        )),
                        node("end", WorkflowNodeType.END, "结束", 722, 120, Map.of(
                                "outputParams", List.of(Map.of("name", "result", "value", "result", "type", "String"))
                        ))
                ),
                List.of(
                        edge("edge-1", "start", "classifier", null),
                        edge("edge-2", "classifier", "after-sales", "index==\"分类1\""),
                        edge("edge-3", "classifier", "other", "index==\"分类2\""),
                        edge("edge-4", "after-sales", "end", null),
                        edge("edge-5", "other", "end", null)
                ),
                List.of()
        );
    }

    private static WorkflowNode node(
            String id,
            WorkflowNodeType type,
            String name,
            int x,
            int y,
            Map<String, Object> config
    ) {
        Map<String, Object> merged = new java.util.LinkedHashMap<>(config);
        merged.put("ui", Map.of("position", Map.of("x", x, "y", y)));
        return new WorkflowNode(id, type, name, merged);
    }

    private static WorkflowEdge edge(String id, String sourceNodeId, String targetNodeId, String condition) {
        return new WorkflowEdge(id, sourceNodeId, targetNodeId, condition);
    }
}
