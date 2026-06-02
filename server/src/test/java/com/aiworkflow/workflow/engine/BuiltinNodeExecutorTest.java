package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
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

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("name", "Ada"));
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

    private WorkflowNode node(String id, WorkflowNodeType type, Map<String, Object> config) {
        return new WorkflowNode(id, type, id, config);
    }
}
