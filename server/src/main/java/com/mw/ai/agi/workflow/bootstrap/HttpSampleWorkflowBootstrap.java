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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class HttpSampleWorkflowBootstrap {
    private static final String SAMPLE_NAME = "HTTP请求测试";
    private static final String TENANT_ID = "tenant_default";
    private static final String CREATED_BY = "system";

    @Bean
    ApplicationRunner seedHttpSampleWorkflow(WorkflowApplicationService workflowService) {
        return args -> {
            if (workflowService.listWorkflows().stream().anyMatch(workflow -> SAMPLE_NAME.equals(workflow.name()))) {
                return;
            }
            Workflow workflow = workflowService.createWorkflow(
                    TENANT_ID,
                    SAMPLE_NAME,
                    "HTTP 请求节点示例：调用 httpbin 并返回响应结果",
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
                                "inputParams", List.of(Map.of("name", "keyword", "type", "String", "required", true)),
                                "defaultInputJson", "{\"keyword\":\"workflow-http-demo\"}"
                        )),
                        node("http", WorkflowNodeType.HTTP_TOOL, "HTTP 请求", 292, 120, Map.of(
                                "method", "GET",
                                "url", "https://httpbin.org/get",
                                "params", List.of(Map.of("key", "q", "value", "{{keyword}}")),
                                "headers", List.of(Map.of("key", "Accept", "value", "application/json")),
                                "responseBodyType", "JSON",
                                "timeoutMs", 15000,
                                "outputKey", "httpResult"
                        )),
                        node("end", WorkflowNodeType.END, "结束", 552, 120, Map.of(
                                "outputParams", List.of(
                                        Map.of("name", "keyword", "value", "keyword", "type", "String"),
                                        Map.of("name", "success", "value", "httpResult.success", "type", "Boolean"),
                                        Map.of("name", "statusCode", "value", "httpResult.statusCode", "type", "Number"),
                                        Map.of("name", "response", "value", "httpResult.body", "type", "Object")
                                )
                        ))
                ),
                List.of(
                        edge("edge-1", "start", "http", null),
                        edge("edge-2", "http", "end", null)
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
        Map<String, Object> merged = new LinkedHashMap<>(config);
        merged.put("ui", Map.of("position", Map.of("x", x, "y", y)));
        return new WorkflowNode(id, type, name, merged);
    }

    private static WorkflowEdge edge(String id, String sourceNodeId, String targetNodeId, String condition) {
        return new WorkflowEdge(id, sourceNodeId, targetNodeId, condition);
    }
}
