package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public HttpToolNodeExecutor(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.HTTP_TOOL;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        return NodeExecutionResult.output(Map.of(optionalStringConfig(node, "outputKey", "toolResult"), executeRequest(node.config(), context.context())));
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
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("headers", responseHeaders(response));
            result.put("statusCode", response.statusCode());
            result.put("body", parseBody(response.body(), stringValue(config.get("responseBodyType"), "TEXT")));
            result.put("rawBody", response.body());
            result.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
            return result;
        } catch (IOException ex) {
            throw new IllegalArgumentException("HTTP tool request failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("HTTP tool request interrupted", ex);
        }
    }

    private Map<String, String> headers(Map<String, Object> config, Map<String, Object> context) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.putAll(rowMap(config.get("headers"), context));
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
