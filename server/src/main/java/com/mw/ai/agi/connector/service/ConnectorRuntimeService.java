package com.mw.ai.agi.connector.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.chat.service.AgentAuditService;
import com.mw.ai.agi.chat.service.HumanConfirmService;
import com.mw.ai.agi.connector.domain.Connector;
import com.mw.ai.agi.connector.domain.ConnectorOperation;
import com.mw.ai.agi.workflow.engine.ConfirmRequiredException;
import com.mw.ai.agi.workflow.engine.TemplateRenderer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

@Service
public class ConnectorRuntimeService {
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final ConnectorService connectorService;
    private final HumanConfirmService humanConfirmService;
    private final AgentAuditService agentAuditService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ConnectorRuntimeService(
            ConnectorService connectorService,
            HumanConfirmService humanConfirmService,
            AgentAuditService agentAuditService,
            ObjectMapper objectMapper
    ) {
        this.connectorService = connectorService;
        this.humanConfirmService = humanConfirmService;
        this.agentAuditService = agentAuditService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public Map<String, Object> execute(
            String connectorCode,
            String operationCode,
            Map<String, Object> nodeConfig,
            Map<String, Object> context,
            String nodeId
    ) {
        Connector connector = connectorService.getConnectorByCode(connectorCode);
        if (!connector.enabled()) {
            throw new IllegalArgumentException("Connector is disabled: " + connectorCode);
        }
        ConnectorOperation operation = connectorService.findOperationByCode(connectorCode, operationCode)
                .orElseThrow(() -> new IllegalArgumentException("Operation not found: " + operationCode));
        if (!operation.enabled()) {
            throw new IllegalArgumentException("Operation is disabled: " + operationCode);
        }

        Map<String, Object> payload = buildPayload(nodeConfig, context, operation);
        String payloadHash = hashPayload(payload);
        String traceId = stringValue(context.get("__traceId"), UUID.randomUUID().toString());

        if (operation.needConfirm() && !isConfirmed(context, payloadHash)) {
            String summary = renderSummary(operation.confirmSummaryTemplate(), context, payload);
            String taskId = humanConfirmService.createTask(
                    stringValue(context.get("__conversationId"), null),
                    stringValue(context.get("botId"), null),
                    stringValue(context.get("__workflowExecutionId"), null),
                    nodeId,
                    stringValue(context.get("userId"), "unknown"),
                    operation.name(),
                    summary,
                    payload,
                    connectorCode,
                    operationCode,
                    payloadHash
            );
            agentAuditService.log(
                    stringValue(context.get("userId"), null),
                    stringValue(context.get("botId"), null),
                    stringValue(context.get("__conversationId"), null),
                    null,
                    connectorCode,
                    operationCode,
                    "CONFIRM_REQUIRED",
                    summary,
                    null,
                    "PENDING",
                    null,
                    traceId
            );
            throw new ConfirmRequiredException(
                    taskId,
                    stringValue(context.get("__workflowExecutionId"), null),
                    nodeId,
                    summary,
                    connectorCode,
                    operationCode
            );
        }

        return doHttpCall(connector, operation, payload, context, traceId);
    }

    public Map<String, Object> testOperation(
            String connectorId,
            String operationId,
            Map<String, Object> testPayload,
            Map<String, Object> context
    ) {
        Connector connector = connectorService.getConnector(connectorId);
        ConnectorOperation operation = connectorService.getOperation(connectorId, operationId);
        Map<String, Object> renderContext = new LinkedHashMap<>(context == null ? Map.of() : context);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (testPayload != null && !testPayload.isEmpty()) {
            payload.putAll(testPayload);
        } else if (operation.requestTemplate() != null && !operation.requestTemplate().isBlank()) {
            String rendered = TemplateRenderer.render(operation.requestTemplate(), renderContext);
            try {
                Map<String, Object> parsed = objectMapper.readValue(rendered, new TypeReference<>() {});
                payload.putAll(parsed);
            } catch (IOException ex) {
                payload.put("body", rendered);
            }
        }
        String traceId = UUID.randomUUID().toString();
        if (renderContext != null) {
            String incoming = stringValue(renderContext.get("__traceId"), "");
            if (!incoming.isBlank()) {
                traceId = incoming;
            }
        }
        return doHttpCall(connector, operation, payload, renderContext, traceId);
    }

    private Map<String, Object> doHttpCall(
            Connector connector,
            ConnectorOperation operation,
            Map<String, Object> payload,
            Map<String, Object> context,
            String traceId
    ) {
        String method = operation.method().toUpperCase();
        String url = joinUrl(connector.baseUrl(), TemplateRenderer.render(operation.path(), context));
        Map<String, String> headers = buildHeaders(connector, context, traceId);
        int timeoutMs = intValue(context.get("__connectorTimeoutMs"), 10000);

        String body = "";
        if (methodRequiresBody(method)) {
            try {
                body = objectMapper.writeValueAsString(payload);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Failed to serialize request payload", ex);
            }
        } else {
            url = appendQuery(url, toStringMap(payload));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs));
        headers.forEach(builder::header);

        if (methodRequiresBody(method)) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (!headers.containsKey("Content-Type")) {
                builder.header("Content-Type", "application/json");
            }
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        try {
            long startedAt = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long durationMs = System.currentTimeMillis() - startedAt;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
            result.put("statusCode", response.statusCode());
            result.put("data", parseBody(response.body()));
            result.put("raw", response.body());
            result.put("traceId", traceId);
            result.put("requestUrl", url);
            result.put("durationMs", durationMs);
            agentAuditService.log(
                    stringValue(context.get("userId"), null),
                    stringValue(context.get("botId"), null),
                    stringValue(context.get("__conversationId"), null),
                    null,
                    connector.code(),
                    operation.code(),
                    "CONNECTOR_CALL",
                    truncate(body),
                    truncate(response.body()),
                    result.get("success").equals(Boolean.TRUE) ? "SUCCESS" : "FAILED",
                    null,
                    traceId
            );
            return result;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            agentAuditService.log(
                    stringValue(context.get("userId"), null),
                    stringValue(context.get("botId"), null),
                    stringValue(context.get("__conversationId"), null),
                    null,
                    connector.code(),
                    operation.code(),
                    "CONNECTOR_CALL",
                    truncate(body),
                    null,
                    "FAILED",
                    ex.getMessage(),
                    traceId
            );
            throw new IllegalArgumentException("Connector call failed: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildPayload(Map<String, Object> nodeConfig, Map<String, Object> context, ConnectorOperation operation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Object inputMapping = nodeConfig.get("inputMapping");
        if (inputMapping instanceof Map<?, ?> mapping) {
            mapping.forEach((key, template) -> payload.put(String.valueOf(key), TemplateRenderer.render(String.valueOf(template), context)));
        } else if (operation.requestTemplate() != null && !operation.requestTemplate().isBlank()) {
            String rendered = TemplateRenderer.render(operation.requestTemplate(), context);
            try {
                Map<String, Object> parsed = objectMapper.readValue(rendered, new TypeReference<>() {});
                payload.putAll(parsed);
            } catch (IOException ex) {
                payload.put("body", rendered);
            }
        }
        return payload;
    }

    private Map<String, String> buildHeaders(Connector connector, Map<String, Object> context, String traceId) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Trace-Id", traceId);
        String authMode = connector.authMode() == null ? "NONE" : connector.authMode();
        switch (authMode) {
            case "USER_TOKEN" -> {
                Object token = context.get("userToken");
                if (token != null && !String.valueOf(token).isBlank()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                putIdentityHeaders(headers, context);
            }
            case "API_KEY" -> applyAuthConfig(headers, connector.authConfig(), "apiKeyHeader", "apiKeyValue");
            case "FIXED_HEADER" -> applyAuthConfig(headers, connector.authConfig(), "headerName", "headerValue");
            default -> putIdentityHeaders(headers, context);
        }
        return headers;
    }

    private void putIdentityHeaders(Map<String, String> headers, Map<String, Object> context) {
        putIfPresent(headers, "X-User-Id", context.get("userId"));
        putIfPresent(headers, "X-Unit-Id", context.get("unitId"));
        putIfPresent(headers, "X-Tenant-Id", context.get("tenantId"));
    }

    private void applyAuthConfig(Map<String, String> headers, String authConfigJson, String nameKey, String valueKey) {
        if (authConfigJson == null || authConfigJson.isBlank()) {
            return;
        }
        try {
            Map<String, String> config = objectMapper.readValue(authConfigJson, STRING_MAP);
            String name = config.get(nameKey);
            String value = config.get(valueKey);
            if (name != null && value != null) {
                headers.put(name, value);
            }
        } catch (IOException ignored) {
        }
    }

    private boolean isConfirmed(Map<String, Object> context, String payloadHash) {
        Object confirmedTaskId = context.get("__confirmedTaskId");
        if (confirmedTaskId == null) {
            return false;
        }
        return humanConfirmService.isConfirmed(String.valueOf(confirmedTaskId), payloadHash);
    }

    private String renderSummary(String template, Map<String, Object> context, Map<String, Object> payload) {
        if (template == null || template.isBlank()) {
            return payload.toString();
        }
        Map<String, Object> renderContext = new LinkedHashMap<>(context);
        renderContext.put("payload", payload);
        return TemplateRenderer.render(template, renderContext);
    }

    private String hashPayload(Map<String, Object> payload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash payload", ex);
        }
    }

    private String joinUrl(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private String appendQuery(String url, Map<String, String> queryParams) {
        if (queryParams.isEmpty()) {
            return url;
        }
        String prefix = url.contains("?") ? "&" : "?";
        StringJoiner joiner = new StringJoiner("&");
        queryParams.forEach((key, value) -> joiner.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));
        return url + prefix + joiner;
    }

    private Map<String, String> toStringMap(Map<String, Object> payload) {
        Map<String, String> result = new LinkedHashMap<>();
        payload.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
        return result;
    }

    private Object parseBody(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, Object.class);
        } catch (IOException ex) {
            return body;
        }
    }

    private boolean methodRequiresBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private void putIfPresent(Map<String, String> headers, String name, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            headers.put(name, String.valueOf(value));
        }
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
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

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
