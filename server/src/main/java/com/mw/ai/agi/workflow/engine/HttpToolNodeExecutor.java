package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.connector.service.ConnectorRuntimeService;
import com.mw.ai.agi.chat.service.AgentAuditService;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Component
public class HttpToolNodeExecutor implements WorkflowNodeExecutor {
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConnectorRuntimeService connectorRuntimeService;
    private final AgentAuditService agentAuditService;

    public HttpToolNodeExecutor(ObjectMapper objectMapper) {
        this(objectMapper, null, null);
    }

    @Autowired
    public HttpToolNodeExecutor(
            ObjectMapper objectMapper,
            ConnectorRuntimeService connectorRuntimeService,
            AgentAuditService agentAuditService
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.connectorRuntimeService = connectorRuntimeService;
        this.agentAuditService = agentAuditService;
    }

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.HTTP_TOOL;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        Map<String, Object> config = node.config();
        String connectorCode = stringValue(config.get("connectorCode"), "");
        String operationCode = stringValue(config.get("operationCode"), "");
        String outputKey = optionalStringConfig(node, "outputKey", "toolResult");
        WorkflowStreamContext.current().ifPresent(sink -> {
            if (!connectorCode.isBlank() || !operationCode.isBlank()) {
                sink.emitToolStarted(connectorCode, operationCode);
            }
        });
        if (!connectorCode.isBlank() && !operationCode.isBlank()) {
            if (connectorRuntimeService == null) {
                throw new IllegalStateException("Connector runtime is unavailable");
            }
            try {
                Map<String, Object> result = connectorRuntimeService.execute(
                        connectorCode,
                        operationCode,
                        config,
                        context.context(),
                        node.id()
                );
                WorkflowStreamContext.current().ifPresent(sink ->
                        sink.emitToolCompleted(connectorCode, operationCode));
                return NodeExecutionResult.output(Map.of(outputKey, result));
            } catch (RuntimeException ex) {
                WorkflowStreamContext.current().ifPresent(sink ->
                        sink.emitToolFailed(connectorCode, operationCode, ex.getMessage()));
                throw ex;
            }
        }
        try {
            Map<String, Object> result = executeRequest(config, context.context());
            WorkflowStreamContext.current().ifPresent(sink ->
                    sink.emitToolCompleted(connectorCode, operationCode));
            return NodeExecutionResult.output(Map.of(outputKey, result));
        } catch (RuntimeException ex) {
            WorkflowStreamContext.current().ifPresent(sink ->
                    sink.emitToolFailed(connectorCode, operationCode, ex.getMessage()));
            throw ex;
        }
    }

    Map<String, Object> executeInline(Map<String, Object> config, Map<String, Object> context) {
        return executeRequest(config, context);
    }

    private Map<String, Object> executeRequest(Map<String, Object> config, Map<String, Object> context) {
        String method = stringValue(config.get("method"), "POST").toUpperCase();
        String url = TemplateRenderer.render(requiredString(config, "url"), context);
        String bodyType = stringValue(config.get("bodyType"), "JSON").toUpperCase();
        int timeoutMs = intValue(config.get("timeoutMs"), 30000);
        Map<String, String> queryParams = rowMap(config.get("params"), context);
        Map<String, String> headers = headers(config, context);
        String body = body(config, context, bodyType);

        URI uri = URI.create(appendQuery(url, queryParams));
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(timeoutMs));
        headers.forEach(builder::header);
        if (methodRequiresBody(method)) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        try {
            long startedAt = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long durationMs = System.currentTimeMillis() - startedAt;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("headers", responseHeaders(response));
            result.put("statusCode", response.statusCode());
            result.put("body", parseBody(response.body(), stringValue(config.get("responseBodyType"), "TEXT")));
            result.put("rawBody", response.body());
            result.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
            result.put("traceId", stringValue(context.get("__traceId"), ""));
            result.put("requestUrl", uri.toString());
            result.put("durationMs", durationMs);
            String status = Boolean.TRUE.equals(result.get("success")) ? "SUCCESS" : "FAILED";
            auditInlineHttp(config, context, truncate(body), truncate(response.body()), status);
            return result;
        } catch (IOException ex) {
            auditInlineHttp(config, context, truncate(body), null, "FAILED");
            throw new IllegalArgumentException("HTTP tool request failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            auditInlineHttp(config, context, truncate(body), null, "FAILED");
            throw new IllegalArgumentException("HTTP tool request interrupted", ex);
        }
    }

    private void auditInlineHttp(
            Map<String, Object> config,
            Map<String, Object> context,
            String requestSummary,
            String responseSummary,
            String status
    ) {
        if (agentAuditService == null) {
            return;
        }
        String url = config.containsKey("url") ? TemplateRenderer.render(String.valueOf(config.get("url")), context) : "";
        String operationCode = shortenUrlForAudit(url);
        String summary = requestSummary;
        if (url != null && !url.isBlank()) {
            summary = "URL: " + url + (summary == null || summary.isBlank() ? "" : "\n" + summary);
        }
        agentAuditService.log(
                stringValue(context.get("userId"), null),
                stringValue(context.get("botId"), null),
                stringValue(context.get("__conversationId"), null),
                null,
                "HTTP_INLINE",
                operationCode,
                "HTTP_TOOL_CALL",
                summary,
                responseSummary,
                status,
                "FAILED".equals(status) ? responseSummary : null,
                stringValue(context.get("__traceId"), null)
        );
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String shortenUrlForAudit(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            String path = URI.create(url).getPath();
            if (path != null && !path.isBlank()) {
                return path.length() > 64 ? path.substring(0, 64) : path;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return url.length() > 64 ? url.substring(0, 64) : url;
    }

    private Map<String, String> headers(Map<String, Object> config, Map<String, Object> context) {
        Map<String, String> headers = new LinkedHashMap<>();
        String traceId = stringValue(context.get("__traceId"), "");
        if (!traceId.isBlank()) {
            headers.put("X-Trace-Id", traceId);
        }
        headers.putAll(rowMap(config.get("headers"), context));
        applyIdentityHeaders(headers, context);
        String headersJson = stringValue(config.get("headersJson"), "");
        if (!headersJson.isBlank()) {
            try {
                Map<String, String> parsed = objectMapper.readValue(TemplateRenderer.render(headersJson, context), STRING_MAP);
                headers.putAll(parsed);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Invalid HTTP headers JSON", ex);
            }
        }
        return headers;
    }

    private void applyIdentityHeaders(Map<String, String> headers, Map<String, Object> context) {
        if (!headers.containsKey("Authorization") && !headers.containsKey("authorization")) {
            Object token = context.get("userToken");
            if (token != null && !String.valueOf(token).isBlank()) {
                headers.put("Authorization", "Bearer " + token);
            }
        }
        putHeaderIfPresent(headers, "X-User-Id", context.get("userId"));
        putHeaderIfPresent(headers, "X-Unit-Id", context.get("unitId"));
        putHeaderIfPresent(headers, "X-Tenant-Id", context.get("tenantId"));
    }

    private void putHeaderIfPresent(Map<String, String> headers, String name, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            headers.put(name, String.valueOf(value));
        }
    }

    private String body(Map<String, Object> config, Map<String, Object> context, String bodyType) {
        if ("NONE".equals(bodyType)) {
            return "";
        }
        if ("FORM_DATA".equals(bodyType) || "FORM".equals(bodyType)) {
            return encodeForm(rowMap(config.get("formData"), context));
        }
        if (config.containsKey("bodyTemplate")) {
            return TemplateRenderer.render(stringValue(config.get("bodyTemplate"), ""), context);
        }
        return TemplateRenderer.render(stringValue(config.get("body"), ""), context);
    }

    private Map<String, String> rowMap(Object value, Map<String, Object> context) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value instanceof List<?> rows) {
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    Object key = map.get("key");
                    Object rowValue = map.get("value");
                    if (key instanceof String keyValue && !keyValue.isBlank()) {
                        result.put(keyValue, TemplateRenderer.render(rowValue == null ? "" : String.valueOf(rowValue), context));
                    }
                }
            }
        }
        return result;
    }

    private String appendQuery(String url, Map<String, String> queryParams) {
        if (queryParams.isEmpty()) {
            return url;
        }
        String joinerPrefix = url.contains("?") ? "&" : "?";
        return url + joinerPrefix + encodeForm(queryParams);
    }

    private String encodeForm(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((key, value) -> joiner.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));
        return joiner.toString();
    }

    private Object parseBody(String body, String responseBodyType) {
        if ("JSON".equalsIgnoreCase(responseBodyType)) {
            try {
                return objectMapper.readValue(body, Object.class);
            } catch (JsonProcessingException ex) {
                throw new IllegalArgumentException("HTTP response body is not valid JSON", ex);
            }
        }
        return body;
    }

    private Map<String, Object> responseHeaders(HttpResponse<String> response) {
        Map<String, Object> headers = new LinkedHashMap<>();
        response.headers().map().forEach((key, values) -> headers.put(key, values.size() == 1 ? values.get(0) : values));
        return headers;
    }

    private boolean methodRequiresBody(String method) {
        return method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
    }

    private String requiredString(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("HTTP node requires config: " + key);
        }
        return stringValue;
    }

    private String optionalStringConfig(WorkflowNode node, String key, String defaultValue) {
        return stringValue(node.config().get(key), defaultValue);
    }

    private String stringValue(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Integer.parseInt(stringValue);
        }
        return defaultValue;
    }
}
