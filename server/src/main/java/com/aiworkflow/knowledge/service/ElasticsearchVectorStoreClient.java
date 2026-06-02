package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeChunkVector;
import com.aiworkflow.knowledge.domain.VectorStoreConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchVectorStoreClient {
    private final ObjectMapper objectMapper;

    public ElasticsearchVectorStoreClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void upsertChunk(VectorStoreConfig config, KnowledgeChunk chunk, KnowledgeChunkVector vector) {
        ensureIndex(config, vector.embedding().size());
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("chunkId", chunk.id());
        document.put("knowledgeBaseId", chunk.knowledgeBaseId());
        document.put("documentId", chunk.documentId());
        document.put("documentName", chunk.documentName());
        document.put("content", chunk.content());
        document.put("chunkIndex", chunk.index());
        document.put("enabled", chunk.enabled());
        document.put("embeddingModelId", vector.embeddingModelId());
        document.put("embedding", vector.embedding());
        send(config, "PUT", "/" + config.indexName() + "/_doc/" + chunk.id(), document, false);
    }

    public List<SearchHit> search(VectorStoreConfig config, String knowledgeBaseId, List<Double> queryEmbedding, int topK) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("size", topK);
        request.put("_source", List.of("chunkId", "documentId", "documentName", "content"));
        request.put("query", Map.of(
                "script_score", Map.of(
                        "query", Map.of(
                                "bool", Map.of(
                                        "filter", List.of(
                                                Map.of("term", Map.of("knowledgeBaseId", knowledgeBaseId)),
                                                Map.of("term", Map.of("enabled", true))
                                        )
                                )
                        ),
                        "script", Map.of(
                                "source", "cosineSimilarity(params.queryVector, 'embedding') + 1.0",
                                "params", Map.of("queryVector", queryEmbedding)
                        )
                )
        ));
        HttpResponse<String> response = send(config, "POST", "/" + config.indexName() + "/_search", request, false);
        try {
            JsonNode hits = objectMapper.readTree(response.body()).path("hits").path("hits");
            List<SearchHit> results = new ArrayList<>();
            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                results.add(new SearchHit(
                        source.path("chunkId").asText(),
                        source.path("documentId").asText(),
                        source.path("documentName").asText(),
                        source.path("content").asText(),
                        hit.path("_score").asDouble()
                ));
            }
            return results;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse Elasticsearch search response", exception);
        }
    }

    public void deleteDocument(VectorStoreConfig config, String knowledgeBaseId, String documentId) {
        deleteByQuery(config, Map.of(
                "bool", Map.of(
                        "filter", List.of(
                                Map.of("term", Map.of("knowledgeBaseId", knowledgeBaseId)),
                                Map.of("term", Map.of("documentId", documentId))
                        )
                )
        ));
    }

    public void deleteKnowledgeBase(VectorStoreConfig config, String knowledgeBaseId) {
        deleteByQuery(config, Map.of("term", Map.of("knowledgeBaseId", knowledgeBaseId)));
    }

    private void deleteByQuery(VectorStoreConfig config, Map<String, Object> query) {
        send(config, "POST", "/" + config.indexName() + "/_delete_by_query?conflicts=proceed", Map.of("query", query), true);
    }

    private void ensureIndex(VectorStoreConfig config, int dimensions) {
        HttpResponse<String> exists = send(config, "HEAD", "/" + config.indexName(), null, true);
        if (exists.statusCode() >= 200 && exists.statusCode() < 300) {
            return;
        }
        if (exists.statusCode() != 404) {
            throw new IllegalStateException("Elasticsearch index check failed: " + exists.statusCode() + " " + exists.body());
        }
        Map<String, Object> mapping = Map.of(
                "mappings", Map.of(
                        "properties", Map.of(
                                "chunkId", Map.of("type", "keyword"),
                                "knowledgeBaseId", Map.of("type", "keyword"),
                                "documentId", Map.of("type", "keyword"),
                                "documentName", Map.of("type", "keyword"),
                                "content", Map.of("type", "text"),
                                "chunkIndex", Map.of("type", "integer"),
                                "enabled", Map.of("type", "boolean"),
                                "embeddingModelId", Map.of("type", "keyword"),
                                "embedding", Map.of("type", "dense_vector", "dims", dimensions, "index", false)
                        )
                )
        );
        send(config, "PUT", "/" + config.indexName(), mapping, false);
    }

    private HttpResponse<String> send(
            VectorStoreConfig config,
            String method,
            String path,
            Object body,
            boolean allowNotFound
    ) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.connectTimeoutMs()))
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri(config, path))
                    .timeout(Duration.ofMillis(config.readTimeoutMs()))
                    .header("Content-Type", "application/json");
            applyAuthorization(config, builder);
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404 && allowNotFound) {
                return response;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Elasticsearch request failed: " + response.statusCode() + " " + response.body());
            }
            return response;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to call Elasticsearch", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch request interrupted", exception);
        }
    }

    private URI uri(VectorStoreConfig config, String path) {
        String endpoint = config.endpoint() == null ? "" : config.endpoint().strip();
        if (endpoint.isBlank()) {
            throw new IllegalStateException("Elasticsearch endpoint is required");
        }
        String normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return URI.create(normalizedEndpoint + path);
    }

    private void applyAuthorization(VectorStoreConfig config, HttpRequest.Builder builder) {
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            builder.header("Authorization", "ApiKey " + config.apiKey());
            return;
        }
        if ((config.username() != null && !config.username().isBlank()) || (config.password() != null && !config.password().isBlank())) {
            String credentials = (config.username() == null ? "" : config.username()) + ":" + (config.password() == null ? "" : config.password());
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
    }

    public record SearchHit(
            String chunkId,
            String documentId,
            String documentName,
            String content,
            double score
    ) {
    }
}
