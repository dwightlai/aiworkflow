package com.mw.ai.agi.workflow.bootstrap;

import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.bot.service.BotService;
import com.mw.ai.agi.connector.domain.Connector;
import com.mw.ai.agi.connector.domain.ConnectorOperation;
import com.mw.ai.agi.connector.service.ConnectorService;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowEdge;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class ConnectorDemoBootstrap {
    private static final Logger log = LoggerFactory.getLogger(ConnectorDemoBootstrap.class);
    private static final String TENANT_ID = "tenant_default";
    private static final String CREATED_BY = "system";
    private static final String CONNECTOR_CODE = "demo_platform";
    private static final String WORKFLOW_NAME = "连接器调用演示";
    private static final String BOT_NAME = "连接器演示助手";

    @Bean
    @Order(25)
    ApplicationRunner seedConnectorDemo(
            ConnectorService connectorService,
            WorkflowApplicationService workflowService,
            BotService botService,
            @Value("${server.port:8080}") int serverPort
    ) {
        return args -> {
            TenantContext.set(TenantContext.normalize(TENANT_ID));
            try {
                String baseUrl = "http://127.0.0.1:" + serverPort;
                Connector connector = ensureConnector(connectorService, baseUrl);
                ensureOperation(connectorService, connector, "greeting", "问候演示", "GET", "/api/demo/connector/greeting");
                ensureOperation(connectorService, connector, "run_demo", "综合演示", "GET", "/api/demo/connector/run");
                String workflowId = upsertWorkflow(workflowService, connectorDefinition());
                upsertBot(botService, workflowId);
                log.info("Connector demo seeded: connector={}, workflow={}, bot={}", CONNECTOR_CODE, WORKFLOW_NAME, BOT_NAME);
            } catch (RuntimeException ex) {
                log.warn("Skip connector demo seed: {}", ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        };
    }

    static WorkflowDefinition connectorDefinition() {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, "开始", 32, 120, Map.of(
                                "inputParams", List.of(
                                        Map.of("name", "message", "type", "String", "required", false),
                                        Map.of("name", "themeLibraryId", "type", "String", "required", false)
                                ),
                                "defaultInputJson", "{\"message\":\"你好\",\"themeLibraryId\":\"theme_001\"}"
                        )),
                        node("connector_call", WorkflowNodeType.HTTP_TOOL, "调用连接器", 292, 120, Map.of(
                                "connectorCode", CONNECTOR_CODE,
                                "operationCode", "run_demo",
                                "inputMapping", Map.of(
                                        "keyword", "{{message}}",
                                        "themeLibraryId", "theme_001"
                                ),
                                "outputKey", "demoResult"
                        )),
                        node("end", WorkflowNodeType.END, "结束", 552, 120, Map.of(
                                "outputParams", List.of(
                                        Map.of("name", "answer", "value", "connector_call.demoResult.data.answer", "type", "String"),
                                        Map.of("name", "itemCount", "value", "connector_call.demoResult.data.itemCount", "type", "Number"),
                                        Map.of("name", "traceId", "value", "connector_call.demoResult.traceId", "type", "String")
                                )
                        ))
                ),
                List.of(
                        edge("edge-1", "start", "connector_call", null),
                        edge("edge-2", "connector_call", "end", null)
                ),
                List.of()
        );
    }

    private Connector ensureConnector(ConnectorService connectorService, String baseUrl) {
        try {
            Connector existing = connectorService.getConnectorByCode(CONNECTOR_CODE);
            return connectorService.updateConnector(
                    existing.id(),
                    "演示平台 API",
                    CONNECTOR_CODE,
                    "GENERIC",
                    "HTTP",
                    baseUrl,
                    "NONE",
                    null,
                    true,
                    "平台内置演示连接器：供工作流 HTTP_TOOL 节点调用 /api/demo/connector 接口"
            );
        } catch (RuntimeException ex) {
            return connectorService.createConnector(
                    "演示平台 API",
                    CONNECTOR_CODE,
                    "GENERIC",
                    "HTTP",
                    baseUrl,
                    "NONE",
                    null,
                    true,
                    "平台内置演示连接器：供工作流 HTTP_TOOL 节点调用 /api/demo/connector 接口"
            );
        }
    }

    private void ensureOperation(
            ConnectorService connectorService,
            Connector connector,
            String code,
            String name,
            String method,
            String path
    ) {
        Optional<ConnectorOperation> existing = connectorService.findOperationByCode(CONNECTOR_CODE, code);
        if (existing.isPresent()) {
            connectorService.updateOperation(
                    connector.id(),
                    existing.get().id(),
                    name,
                    code,
                    method,
                    path,
                    "QUERY",
                    "LOW",
                    false,
                    null,
                    null,
                    true,
                    "演示接口：" + path
            );
            return;
        }
        connectorService.createOperation(
                connector.id(),
                name,
                code,
                method,
                path,
                "QUERY",
                "LOW",
                false,
                null,
                null,
                true,
                "演示接口：" + path
        );
    }

    private String upsertWorkflow(WorkflowApplicationService workflowService, WorkflowDefinition definition) {
        Optional<Workflow> existing = workflowService.listWorkflows().stream()
                .filter(workflow -> WORKFLOW_NAME.equals(workflow.name()))
                .findFirst();
        if (existing.isPresent()) {
            workflowService.updateDraftDefinition(existing.get().id(), definition);
            workflowService.publishDraftVersion(existing.get().id(), CREATED_BY);
            return existing.get().id();
        }
        Workflow workflow = workflowService.createWorkflow(
                TENANT_ID,
                WORKFLOW_NAME,
                "演示工作流通过 HTTP_TOOL 节点调用 demo_platform 连接器",
                CREATED_BY,
                definition
        );
        workflowService.publishDraftVersion(workflow.id(), CREATED_BY);
        return workflow.id();
    }

    private void upsertBot(BotService botService, String workflowId) {
        Optional<AiBot> existing = botService.list(Map.of()).stream()
                .filter(bot -> BOT_NAME.equals(bot.name()))
                .findFirst();
        if (existing.isPresent()) {
            botService.update(
                    existing.get().id(),
                    BOT_NAME,
                    "演示 Bot → 工作流 → HTTP_TOOL → demo_platform 连接器全链路",
                    existing.get().ownerUnitId(),
                    existing.get().avatar(),
                    workflowId,
                    null,
                    List.of(),
                    "你是连接器演示助手，会调用平台内置 demo_platform 连接器返回资料摘要。",
                    "你好，我是连接器演示助手。随便说点什么，我会通过工作流调用 demo_platform.run_demo 接口并返回结果。",
                    "演示：工作流 HTTP_TOOL 节点 → 连接器 → 平台 Demo API",
                    List.of("查询主题库资料", "连接器怎么工作的？"),
                    BotStatus.ENABLED
            );
            return;
        }
        botService.create(
                BOT_NAME,
                "演示 Bot → 工作流 → HTTP_TOOL → demo_platform 连接器全链路",
                null,
                "api",
                workflowId,
                null,
                List.of(),
                "你是连接器演示助手，会调用平台内置 demo_platform 连接器返回资料摘要。",
                "你好，我是连接器演示助手。随便说点什么，我会通过工作流调用 demo_platform.run_demo 接口并返回结果。",
                "演示：工作流 HTTP_TOOL 节点 → 连接器 → 平台 Demo API",
                List.of("查询主题库资料", "连接器怎么工作的？"),
                BotStatus.ENABLED
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
