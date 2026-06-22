package com.mw.ai.agi.workflow.bootstrap;

import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotCapability;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.bot.service.BotCapabilityService;
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
public class ArchiveOfficeRouteDemoBootstrap {
    private static final Logger log = LoggerFactory.getLogger(ArchiveOfficeRouteDemoBootstrap.class);
    private static final String TENANT_ID = "tenant_default";
    private static final String CREATED_BY = "system";
    private static final String CONNECTOR_CODE = "archive_office";
    private static final String WORKFLOW_BORROW_NAME = "档案借阅办理";
    private static final String WORKFLOW_UTILIZE_NAME = "档案利用咨询";
    private static final String WORKFLOW_FILE_QUERY_NAME = "档案文件查询";
    private static final String BOT_NAME = "档案办公助手";
    private static final String CAP_BORROW = "archive_borrow";
    private static final String CAP_UTILIZE = "archive_utilize";
    private static final String CAP_FILE_QUERY = "archive_file_query";

    @Bean
    @Order(26)
    ApplicationRunner seedArchiveOfficeRouteDemo(
            ConnectorService connectorService,
            WorkflowApplicationService workflowService,
            BotService botService,
            BotCapabilityService botCapabilityService,
            @Value("${server.port:8080}") int serverPort
    ) {
        return args -> {
            TenantContext.set(TenantContext.normalize(TENANT_ID));
            try {
                String baseUrl = "http://127.0.0.1:" + serverPort;
                Connector connector = ensureConnector(connectorService, baseUrl);
                ensureOperation(connectorService, connector, "borrow_apply", "档案借阅", "GET", "/api/demo/archive/borrow");
                ensureOperation(connectorService, connector, "utilize_query", "档案利用", "GET", "/api/demo/archive/utilize");
                ensureOperation(connectorService, connector, "file_query", "档案文件查询", "GET", "/api/demo/archive/files");
                String borrowWorkflowId = upsertWorkflow(
                        workflowService,
                        WORKFLOW_BORROW_NAME,
                        "档案借阅场景：HTTP_TOOL 调用 archive_office.borrow_apply",
                        borrowDefinition()
                );
                String utilizeWorkflowId = upsertWorkflow(
                        workflowService,
                        WORKFLOW_UTILIZE_NAME,
                        "档案利用场景：HTTP_TOOL 调用 archive_office.utilize_query",
                        utilizeDefinition()
                );
                String fileQueryWorkflowId = upsertWorkflow(
                        workflowService,
                        WORKFLOW_FILE_QUERY_NAME,
                        "档案文件查询场景：HTTP_TOOL 调用 archive_office.file_query",
                        fileQueryDefinition()
                );
                String botId = upsertBot(botService, borrowWorkflowId);
                ensureCapability(botCapabilityService, botId, borrowWorkflowId, CAP_BORROW, "借阅,借档,调档,出库,原件", true);
                ensureCapability(botCapabilityService, botId, utilizeWorkflowId, CAP_UTILIZE, "利用,查阅,复制,摘录,开放,查档", false);
                ensureCapability(botCapabilityService, botId, fileQueryWorkflowId, CAP_FILE_QUERY, "查询,检索,查找,找一下,查一下,找,文件,题名,档号,找档案,搜索", false);
                log.info("Archive office route demo seeded: bot={}, workflows=[{}, {}, {}]",
                        BOT_NAME, WORKFLOW_BORROW_NAME, WORKFLOW_UTILIZE_NAME, WORKFLOW_FILE_QUERY_NAME);
            } catch (RuntimeException ex) {
                log.warn("Skip archive office route demo seed: {}", ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        };
    }

    static WorkflowDefinition borrowDefinition() {
        return workflowDefinition(
                "borrow_call",
                "借阅接口",
                "borrowResult",
                "borrow_call.borrowResult.data.answer",
                "borrow_call.borrowResult.data.status",
                "borrow_apply",
                "我想借阅一份合同档案"
        );
    }

    static WorkflowDefinition utilizeDefinition() {
        return workflowDefinition(
                "utilize_call",
                "利用接口",
                "utilizeResult",
                "utilize_call.utilizeResult.data.answer",
                "utilize_call.utilizeResult.data.scene",
                "utilize_query",
                "我想查阅开放档案"
        );
    }

    static WorkflowDefinition fileQueryDefinition() {
        return workflowDefinition(
                "file_query_call",
                "文件查询接口",
                "fileQueryResult",
                "file_query_call.fileQueryResult.data.answer",
                "file_query_call.fileQueryResult.data.scene",
                "file_query",
                "帮我查询信息化建设合同档案"
        );
    }

    private static WorkflowDefinition workflowDefinition(
            String httpNodeId,
            String httpNodeName,
            String outputKey,
            String answerPath,
            String extraPath,
            String operationCode,
            String defaultMessage
    ) {
        return new WorkflowDefinition(
                List.of(
                        node("start", WorkflowNodeType.START, "开始", 32, 120, Map.of(
                                "inputParams", List.of(
                                        Map.of("name", "message", "type", "String", "required", false)
                                ),
                                "defaultInputJson", "{\"message\":\"" + defaultMessage + "\"}"
                        )),
                        node(httpNodeId, WorkflowNodeType.HTTP_TOOL, httpNodeName, 292, 120, Map.of(
                                "connectorCode", CONNECTOR_CODE,
                                "operationCode", operationCode,
                                "inputMapping", Map.of("keyword", "{{message}}"),
                                "outputKey", outputKey
                        )),
                        node("end", WorkflowNodeType.END, "结束", 552, 120, Map.of(
                                "outputParams", List.of(
                                        Map.of("name", "answer", "value", answerPath, "type", "String"),
                                        Map.of("name", "scene", "value", extraPath, "type", "String")
                                )
                        ))
                ),
                List.of(
                        edge("edge-1", "start", httpNodeId, null),
                        edge("edge-2", httpNodeId, "end", null)
                ),
                List.of()
        );
    }

    private Connector ensureConnector(ConnectorService connectorService, String baseUrl) {
        try {
            Connector existing = connectorService.getConnectorByCode(CONNECTOR_CODE);
            return connectorService.updateConnector(
                    existing.id(),
                    "档案办公业务 API",
                    CONNECTOR_CODE,
                    "GENERIC",
                    "HTTP",
                    baseUrl,
                    "NONE",
                    null,
                    true,
                    "档案借阅/利用演示连接器"
            );
        } catch (RuntimeException ex) {
            return connectorService.createConnector(
                    "档案办公业务 API",
                    CONNECTOR_CODE,
                    "GENERIC",
                    "HTTP",
                    baseUrl,
                    "NONE",
                    null,
                    true,
                    "档案借阅/利用演示连接器"
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

    private String upsertWorkflow(
            WorkflowApplicationService workflowService,
            String name,
            String description,
            WorkflowDefinition definition
    ) {
        Optional<Workflow> existing = workflowService.listWorkflows().stream()
                .filter(workflow -> name.equals(workflow.name()))
                .findFirst();
        if (existing.isPresent()) {
            workflowService.updateDraftDefinition(existing.get().id(), definition);
            workflowService.publishDraftVersion(existing.get().id(), CREATED_BY);
            return existing.get().id();
        }
        Workflow workflow = workflowService.createWorkflow(TENANT_ID, name, description, CREATED_BY, definition);
        workflowService.publishDraftVersion(workflow.id(), CREATED_BY);
        return workflow.id();
    }

    private String upsertBot(BotService botService, String primaryWorkflowId) {
        Optional<AiBot> existing = botService.list(Map.of()).stream()
                .filter(bot -> BOT_NAME.equals(bot.name()))
                .findFirst();
        if (existing.isPresent()) {
            botService.update(
                    existing.get().id(),
                    BOT_NAME,
                    "档案办公多工作流路由演示：按关键词自动选择借阅、利用或文件查询工作流",
                    existing.get().ownerUnitId(),
                    existing.get().avatar(),
                    primaryWorkflowId,
                    null,
                    List.of(),
                    "你是档案馆办公助手，根据用户意图引导档案借阅、档案利用或档案文件查询，并返回办理指引。",
                    "你好，我是档案办公助手。你可以咨询档案借阅、档案利用或档案文件查询，我会自动匹配对应工作流。",
                    "演示：多工作流路由 → HTTP_TOOL → archive_office 连接器",
                    List.of("我要借阅合同档案", "如何办理档案利用登记？", "帮我查询信息化建设合同档案"),
                    BotStatus.ENABLED
            );
            return existing.get().id();
        }
        AiBot created = botService.create(
                BOT_NAME,
                "档案办公多工作流路由演示：按关键词自动选择借阅、利用或文件查询工作流",
                null,
                "folder",
                primaryWorkflowId,
                null,
                List.of(),
                "你是档案馆办公助手，根据用户意图引导档案借阅、档案利用或档案文件查询，并返回办理指引。",
                "你好，我是档案办公助手。你可以咨询档案借阅、档案利用或档案文件查询，我会自动匹配对应工作流。",
                "演示：多工作流路由 → HTTP_TOOL → archive_office 连接器",
                List.of("我要借阅合同档案", "如何办理档案利用登记？", "帮我查询信息化建设合同档案"),
                BotStatus.ENABLED
        );
        return created.id();
    }

    private void ensureCapability(
            BotCapabilityService botCapabilityService,
            String botId,
            String workflowId,
            String capabilityCode,
            String routingKeywords,
            boolean primary
    ) {
        Optional<BotCapability> existing = botCapabilityService.listByBot(botId).stream()
                .filter(item -> capabilityCode.equals(item.capabilityCode()))
                .findFirst();
        if (existing.isPresent()) {
            botCapabilityService.update(
                    botId,
                    existing.get().id(),
                    "WORKFLOW",
                    workflowId,
                    capabilityCode,
                    routingKeywords,
                    primary,
                    true
            );
            return;
        }
        botCapabilityService.save(botId, "WORKFLOW", workflowId, capabilityCode, routingKeywords, primary, true);
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
