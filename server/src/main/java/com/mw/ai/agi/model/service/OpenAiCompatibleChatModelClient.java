package com.mw.ai.agi.model.service;

import com.mw.ai.agi.model.domain.ModelProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OpenAiCompatibleChatModelClient implements ChatModelClient {
    private final ModelProviderService modelProviderService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public OpenAiCompatibleChatModelClient(ModelProviderService modelProviderService, ObjectMapper objectMapper) {
        this(modelProviderService, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    OpenAiCompatibleChatModelClient(
            ModelProviderService modelProviderService,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.modelProviderService = modelProviderService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String generate(String providerId, String model, String prompt, Map<String, Object> options) {
        ModelProvider provider = modelProviderService.get(providerId);
        validateProvider(provider);
        String actualModel = firstNonBlank(provider.model(), model);
        if (actualModel == null) {
            throw new IllegalStateException("Chat model is required for provider: " + providerId);
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", actualModel);
        request.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt == null ? "" : prompt)
        });
        if (options != null) {
            copySupportedOption(options, request, "temperature");
            copySupportedOption(options, request, "top_p");
            copySupportedOption(options, request, "max_tokens");
            copySupportedOption(options, request, "presence_penalty");
            copySupportedOption(options, request, "frequency_penalty");
        }

        HttpResponse<String> response = postJson(normalizeEndpoint(provider.baseUrl()) + "/chat/completions", provider.apiKeyRef(), request);
        assertSuccessful(response);
        return parseAnswer(response.body());
    }

    private HttpResponse<String> postJson(String uri, String apiKey, Map<String, Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(body)));
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey.strip());
            }
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to call chat model provider", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chat model provider call was interrupted", exception);
        }
    }

    private String parseAnswer(String body) {
        try {
            JsonNode choices = objectMapper.readTree(body).path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("Chat model response does not contain choices");
            }
            String content = choices.get(0).path("message").path("content").asText(null);
            if (content == null) {
                content = choices.get(0).path("text").asText(null);
            }
            if (content == null) {
                throw new IllegalStateException("Chat model response does not contain content");
            }
            return content;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse chat model response", exception);
        }
    }

    private void assertSuccessful(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Chat model provider request failed: "
                    + response.statusCode() + " " + response.body());
        }
    }

    private void validateProvider(ModelProvider provider) {
        if (!provider.enabled()) {
            throw new IllegalStateException("Chat model provider is disabled: " + provider.id());
        }
        if (!"CHAT".equalsIgnoreCase(provider.modelUsage()) && !"MULTIMODAL".equalsIgnoreCase(provider.modelUsage())) {
            throw new IllegalStateException("Model provider is not configured for chat: " + provider.id());
        }
        if (provider.baseUrl() == null || provider.baseUrl().isBlank()) {
            throw new IllegalStateException("Chat model provider base URL is required: " + provider.id());
        }
    }

    private void copySupportedOption(Map<String, Object> options, Map<String, Object> request, String key) {
        Object value = options.get(key);
        if (value != null) {
            request.put(key, value);
        }
    }

    private String normalizeEndpoint(String endpoint) {
        String normalized = endpoint.strip();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.strip();
        }
        if (second != null && !second.isBlank()) {
            return second.strip();
        }
        return null;
    }
}
