package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.service.ModelProviderService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ModelProviderEmbeddingClient implements EmbeddingClient {
    private final ModelProviderService modelProviderService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ModelProviderEmbeddingClient(ModelProviderService modelProviderService, ObjectMapper objectMapper) {
        this(modelProviderService, objectMapper, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    ModelProviderEmbeddingClient(
            ModelProviderService modelProviderService,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.modelProviderService = modelProviderService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public List<Double> embed(String providerId, String model, String text) {
        ModelProvider provider = modelProviderService.get(providerId);
        validateProvider(provider);
        String actualModel = firstNonBlank(provider.model(), model);
        if (actualModel == null) {
            throw new IllegalStateException("Embedding model is required for provider: " + providerId);
        }
        if ("OLLAMA".equalsIgnoreCase(provider.modelType())) {
            return embedWithOllama(provider, actualModel, text == null ? "" : text);
        }
        if (isOpenAiCompatible(provider.modelType())) {
            return embedWithOpenAiCompatible(provider, actualModel, text == null ? "" : text);
        }
        throw new IllegalStateException("Unsupported embedding model type: " + provider.modelType());
    }

    @Override
    public List<List<Double>> embedAll(String providerId, String model, List<String> texts) {
        ModelProvider provider = modelProviderService.get(providerId);
        validateProvider(provider);
        String actualModel = firstNonBlank(provider.model(), model);
        if (actualModel == null) {
            throw new IllegalStateException("Embedding model is required for provider: " + providerId);
        }
        if (!"OLLAMA".equalsIgnoreCase(provider.modelType())) {
            if (isOpenAiCompatible(provider.modelType())) {
                return embedAllWithOpenAiCompatible(provider, actualModel, texts);
            }
            return EmbeddingClient.super.embedAll(providerId, model, texts);
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", actualModel);
        request.put("input", texts.stream().map(text -> text == null ? "" : text).toList());
        HttpResponse<String> response = postJson(normalizeEndpoint(provider.baseUrl()) + "/api/embed", request);
        assertSuccessful(response, "/api/embed");
        return parseOllamaBatchEmbeddings(response.body());
    }

    private List<Double> embedWithOpenAiCompatible(ModelProvider provider, String model, String text) {
        return embedAllWithOpenAiCompatible(provider, model, List.of(text)).get(0);
    }

    private List<List<Double>> embedAllWithOpenAiCompatible(
            ModelProvider provider,
            String model,
            List<String> texts
    ) {
        List<String> normalizedTexts = texts.stream()
                .map(text -> text == null ? "" : text)
                .toList();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("input", normalizedTexts.size() == 1 ? normalizedTexts.get(0) : normalizedTexts);
        String endpoint = openAiEmbeddingEndpoint(provider.baseUrl());
        HttpResponse<String> response = postJson(endpoint, provider.apiKeyRef(), request);
        assertSuccessful(response, endpoint);
        return parseOpenAiEmbeddings(response.body());
    }

    private List<Double> embedWithOllama(ModelProvider provider, String model, String text) {
        String endpoint = normalizeEndpoint(provider.baseUrl());
        Map<String, Object> embeddingsRequest = new LinkedHashMap<>();
        embeddingsRequest.put("model", model);
        embeddingsRequest.put("prompt", text);
        HttpResponse<String> response = postJson(endpoint + "/api/embeddings", embeddingsRequest);
        if (response.statusCode() == 404) {
            Map<String, Object> embedRequest = new LinkedHashMap<>();
            embedRequest.put("model", model);
            embedRequest.put("input", text);
            response = postJson(endpoint + "/api/embed", embedRequest);
            assertSuccessful(response, "/api/embed");
            return parseOllamaBatchEmbedding(response.body());
        }
        assertSuccessful(response, "/api/embeddings");
        return parseEmbedding(response.body(), "embedding");
    }

    private HttpResponse<String> postJson(String uri, Map<String, Object> body) {
        return postJson(uri, null, body);
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
            throw new IllegalStateException("Unable to call embedding provider", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding provider call was interrupted", exception);
        }
    }

    private List<List<Double>> parseOpenAiEmbeddings(String body) {
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new IllegalStateException("OpenAI-compatible embedding response does not contain data");
            }
            List<List<Double>> results = new ArrayList<>(data.size());
            for (JsonNode item : data) {
                results.add(toDoubleList(item.path("embedding")));
            }
            return results;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse OpenAI-compatible embedding response", exception);
        }
    }

    private List<Double> parseOllamaBatchEmbedding(String body) {
        return parseOllamaBatchEmbeddings(body).get(0);
    }

    private List<List<Double>> parseOllamaBatchEmbeddings(String body) {
        try {
            JsonNode embeddings = objectMapper.readTree(body).path("embeddings");
            if (!embeddings.isArray() || embeddings.isEmpty()) {
                throw new IllegalStateException("Ollama embedding response does not contain embeddings");
            }
            List<List<Double>> results = new ArrayList<>(embeddings.size());
            for (JsonNode embedding : embeddings) {
                results.add(toDoubleList(embedding));
            }
            return results;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse Ollama embedding response", exception);
        }
    }

    private List<Double> parseEmbedding(String body, String fieldName) {
        try {
            return toDoubleList(objectMapper.readTree(body).path(fieldName));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse embedding response", exception);
        }
    }

    private List<Double> toDoubleList(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            throw new IllegalStateException("Embedding response does not contain a vector");
        }
        List<Double> embedding = new ArrayList<>(node.size());
        for (JsonNode value : node) {
            embedding.add(value.asDouble());
        }
        return embedding;
    }

    private void assertSuccessful(HttpResponse<String> response, String path) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Embedding provider request failed at " + path + ": "
                    + response.statusCode() + " " + response.body());
        }
    }

    private void validateProvider(ModelProvider provider) {
        if (!provider.enabled()) {
            throw new IllegalStateException("Embedding provider is disabled: " + provider.id());
        }
        if (!"EMBEDDING".equalsIgnoreCase(provider.modelUsage())) {
            throw new IllegalStateException("Model provider is not configured for embeddings: " + provider.id());
        }
        if (provider.baseUrl() == null || provider.baseUrl().isBlank()) {
            throw new IllegalStateException("Embedding provider base URL is required: " + provider.id());
        }
    }

    private String normalizeEndpoint(String endpoint) {
        String normalized = endpoint.strip();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String openAiEmbeddingEndpoint(String baseUrl) {
        String endpoint = normalizeEndpoint(baseUrl);
        if (endpoint.endsWith("/embeddings")) {
            return endpoint;
        }
        if (endpoint.endsWith("/v1")) {
            return endpoint + "/embeddings";
        }
        return endpoint + "/v1/embeddings";
    }

    private boolean isOpenAiCompatible(String modelType) {
        return "CUSTOM".equalsIgnoreCase(modelType) || "OPENAI".equalsIgnoreCase(modelType);
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
