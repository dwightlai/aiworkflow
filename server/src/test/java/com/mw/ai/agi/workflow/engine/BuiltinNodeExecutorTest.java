package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.model.service.ModelProviderService;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinNodeExecutorTest {

    @Test
    void startNodePassesInputThrough() {
        StartNodeExecutor executor = new StartNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada")
        );

        NodeExecutionResult result = executor.execute(node("start", WorkflowNodeType.START, Map.of()), context);

        assertThat(result.output()).containsEntry("name", "Ada");
        assertThat(result.output()).containsEntry("start", Map.of("name", "Ada"));
        assertThat(result.nextNodeId()).isEmpty();
    }

    @Test
    void endNodeReturnsConfiguredOutputKeys() {
        EndNodeExecutor executor = new EndNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada", "message", "Hello Ada", "internal", "hidden")
        );

        NodeExecutionResult result = executor.execute(node(
                "end",
                WorkflowNodeType.END,
                Map.of("outputKeys", List.of("message", "missing"))
        ), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("message", "Hello Ada"));
        assertThat(result.nextNodeId()).isEmpty();
    }

    @Test
    void endNodeMapsOutputParamsFromNodeScopedVariables() {
        EndNodeExecutor executor = new EndNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("input", "hello"),
                Map.of("LLM大模型", Map.of("content", "model answer"))
        );

        NodeExecutionResult result = executor.execute(node(
                "end",
                WorkflowNodeType.END,
                Map.of("outputParams", List.of(Map.of("name", "result", "value", "LLM大模型.content", "type", "String")))
        ), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("result", "model answer"));
    }

    @Test
    void textTransformNodeRendersTemplateFromContext() {
        TextTransformNodeExecutor executor = new TextTransformNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada", "company", "Lovelace Labs")
        );

        NodeExecutionResult result = executor.execute(node(
                "transform",
                WorkflowNodeType.TEXT_TRANSFORM,
                Map.of("outputKey", "message", "template", "Hello {{name}} from {{company}}")
        ), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("message", "Hello Ada from Lovelace Labs"));
        assertThat(result.nextNodeId()).isEmpty();
    }

    @Test
    void textTransformNodeRendersArrayIndexTemplateFromContext() {
        TextTransformNodeExecutor executor = new TextTransformNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of(),
                Map.of("items", List.of("北京", "上海"))
        );

        NodeExecutionResult result = executor.execute(node(
                "transform",
                WorkflowNodeType.TEXT_TRANSFORM,
                Map.of("outputKey", "message", "template", "城市：${items[1]}")
        ), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("message", "城市：上海"));
    }

    @Test
    void contentTemplateNodeRendersTextOutput() {
        ContentTemplateNodeExecutor executor = new ContentTemplateNodeExecutor(new ObjectMapper());
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada", "company", "Lovelace Labs")
        );

        NodeExecutionResult result = executor.execute(node(
                "template",
                WorkflowNodeType.CONTENT_TEMPLATE,
                Map.of("outputKey", "content", "template", "Hello {{name}} from {{company}}", "outputFormat", "TEXT")
        ), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("content", "Hello Ada from Lovelace Labs"));
    }

    @Test
    void contentTemplateNodeParsesJsonOutput() {
        ContentTemplateNodeExecutor executor = new ContentTemplateNodeExecutor(new ObjectMapper());
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada")
        );

        NodeExecutionResult result = executor.execute(node(
                "template",
                WorkflowNodeType.CONTENT_TEMPLATE,
                Map.of("outputKey", "payload", "template", "{\"name\":\"{{name}}\"}", "outputFormat", "JSON")
        ), context);

        assertThat(result.output()).containsKey("payload");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) result.output().get("payload");
        assertThat(payload).containsExactlyEntriesOf(Map.of("name", "Ada"));
    }

    @Test
    void conditionNodeSelectsMatchedBranch() {
        ConditionNodeExecutor executor = new ConditionNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("tier", "vip"),
                Map.of("tier", "vip")
        );

        NodeExecutionResult result = executor.execute(node(
                "condition",
                WorkflowNodeType.CONDITION,
                Map.of(
                        "contextKey", "tier",
                        "equals", "vip",
                        "trueTargetNodeId", "vip-path",
                        "falseTargetNodeId", "normal-path"
                )
        ), context);

        assertThat(result.output()).isEmpty();
        assertThat(result.nextNodeId()).contains("vip-path");
    }

    @Test
    void conditionNodeSupportsContainsOperator() {
        ConditionNodeExecutor executor = new ConditionNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("answer", "审批通过，请继续"),
                Map.of("answer", "审批通过，请继续")
        );

        NodeExecutionResult result = executor.execute(node(
                "condition",
                WorkflowNodeType.CONDITION,
                Map.of(
                        "contextKey", "answer",
                        "operator", "CONTAINS",
                        "compareValue", "通过",
                        "trueTargetNodeId", "approved",
                        "falseTargetNodeId", "rejected"
                )
        ), context);

        assertThat(result.nextNodeId()).contains("approved");
    }

    @Test
    void httpToolNodeCallsExternalEndpointWithRenderedTemplate() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/echo", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = ("{\"received\":" + requestBody + "}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            HttpToolNodeExecutor executor = new HttpToolNodeExecutor(new ObjectMapper());
            NodeExecutionContext context = new NodeExecutionContext(
                    Map.of("question", "refund"),
                    Map.of("question", "refund")
            );

            NodeExecutionResult result = executor.execute(node(
                    "http",
                    WorkflowNodeType.HTTP_TOOL,
                    Map.of(
                            "method", "POST",
                            "url", "http://127.0.0.1:" + server.getAddress().getPort() + "/echo",
                            "headersJson", "{\"Content-Type\":\"application/json\"}",
                            "bodyTemplate", "{\"question\":\"{{question}}\"}",
                            "outputKey", "toolResult"
                    )
            ), context);

            assertThat(result.output()).containsKey("toolResult");
            @SuppressWarnings("unchecked")
            Map<String, Object> toolResult = (Map<String, Object>) result.output().get("toolResult");
            assertThat(toolResult.get("statusCode")).isEqualTo(200);
            assertThat(toolResult.get("success")).isEqualTo(true);
            assertThat((String) toolResult.get("body")).contains("refund");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void httpToolNodeSupportsParamsHeadersAndJsonResponse() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/search", exchange -> {
            String token = exchange.getRequestHeaders().getFirst("X-Token");
            byte[] response = ("{\"query\":\"" + exchange.getRequestURI().getQuery() + "\",\"token\":\"" + token + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            HttpToolNodeExecutor executor = new HttpToolNodeExecutor(new ObjectMapper());
            NodeExecutionContext context = new NodeExecutionContext(
                    Map.of("keyword", "refund"),
                    Map.of("keyword", "refund", "token", "dev-token")
            );

            NodeExecutionResult result = executor.execute(node(
                    "http",
                    WorkflowNodeType.HTTP_TOOL,
                    Map.of(
                            "method", "GET",
                            "url", "http://127.0.0.1:" + server.getAddress().getPort() + "/search",
                            "params", List.of(Map.of("key", "q", "value", "{{keyword}}")),
                            "headers", List.of(Map.of("key", "X-Token", "value", "{{token}}")),
                            "responseBodyType", "JSON",
                            "outputKey", "httpResult"
                    )
            ), context);

            @SuppressWarnings("unchecked")
            Map<String, Object> toolResult = (Map<String, Object>) result.output().get("httpResult");
            assertThat(toolResult.get("statusCode")).isEqualTo(200);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) toolResult.get("body");
            assertThat(body.get("query")).isEqualTo("q=refund");
            assertThat(body.get("token")).isEqualTo("dev-token");
            assertThat(toolResult).containsKeys("headers", "rawBody", "success");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void loopNodeRunsTemplateStepsForEachItem() {
        LoopNodeExecutor executor = new LoopNodeExecutor(
                new ContentTemplateNodeExecutor(new ObjectMapper()),
                new HttpToolNodeExecutor(new ObjectMapper()),
                null
        );
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("items", List.of("北京", "上海")),
                Map.of("items", List.of("北京", "上海"))
        );

        NodeExecutionResult result = executor.execute(node(
                "loop",
                WorkflowNodeType.LOOP,
                Map.of(
                        "loopVar", "items",
                        "itemVar", "loopItem",
                        "indexVar", "index",
                        "outputKey", "loopResults",
                        "loopSteps", List.of(Map.of(
                                "type", "CONTENT_TEMPLATE",
                                "template", "{{index}}-{{loopItem}}",
                                "outputKey", "text"
                        ))
                )
        ), context);

        assertThat(result.output()).containsKey("loopResults");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> loopResults = (List<Map<String, Object>>) result.output().get("loopResults");
        assertThat(loopResults).extracting(item -> item.get("text")).containsExactly("0-北京", "1-上海");
    }

    @Test
    void questionClassifierNodeMatchesCategoryByKeyword() {
        QuestionClassifierNodeExecutor executor = new QuestionClassifierNodeExecutor(
                (providerId, model, prompt, options) -> "",
                new ModelProviderService()
        );
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("message", "我要申请退款"),
                Map.of("message", "我要申请退款")
        );

        NodeExecutionResult result = executor.execute(node(
                "classifier",
                WorkflowNodeType.QUESTION_CLASSIFIER,
                Map.of(
                        "contentTemplate", "${message}",
                        "outputKey", "index",
                        "categories", List.of(
                                Map.of(
                                        "id", "分类1",
                                        "name", "售后咨询",
                                        "keywords", List.of("退款"),
                                        "matchMode", "CONTAINS"
                                ),
                                Map.of(
                                        "id", "分类2",
                                        "name", "其他问题",
                                        "keywords", List.of("天气"),
                                        "matchMode", "CONTAINS"
                                )
                        )
                )
        ), context);

        assertThat(result.output()).containsEntry("index", "分类1");
    }

    private WorkflowNode node(String id, WorkflowNodeType type, Map<String, Object> config) {
        return new WorkflowNode(id, type, id, config);
    }
}
