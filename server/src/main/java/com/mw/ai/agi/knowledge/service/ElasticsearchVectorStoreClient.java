package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
        EsResponse response = send(config, "POST", "/" + config.indexName() + "/_search", request, false);
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

    public void ensureIndex(VectorStoreConfig config, int dimensions) {
        EsResponse exists = send(config, "HEAD", "/" + config.indexName(), null, true);
        if (exists.statusCode() >= 200 && exists.statusCode() < 300) {
            int currentDimensions = embeddingDimensions(config);
            if (currentDimensions == dimensions) {
                return;
            }
            long documentCount = countDocuments(config);
            if (documentCount > 0) {
                throw new IllegalStateException("Elasticsearch index embedding dimensions mismatch: current "
                        + currentDimensions + ", requested " + dimensions + ". Reindex is required.");
            }
            send(config, "DELETE", "/" + config.indexName(), null, false);
            createIndex(config, dimensions);
            return;
        }
        if (exists.statusCode() != 404) {
            throw new IllegalStateException("Elasticsearch index check failed: " + exists.statusCode() + " " + exists.body());
        }
        createIndex(config, dimensions);
    }

    private void createIndex(VectorStoreConfig config, int dimensions) {
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

    private int embeddingDimensions(VectorStoreConfig config) {
        EsResponse response = send(config, "GET", "/" + config.indexName() + "/_mapping", null, false);
        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode indexNode = root.path(config.indexName());
            if (indexNode.isMissingNode() && root.fields().hasNext()) {
                indexNode = root.fields().next().getValue();
            }
            int dimensions = indexNode.path("mappings")
                    .path("properties")
                    .path("embedding")
                    .path("dims")
                    .asInt(0);
            if (dimensions <= 0) {
                throw new IllegalStateException("Elasticsearch index does not contain embedding dimensions");
            }
            return dimensions;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse Elasticsearch mapping response", exception);
        }
    }

    private long countDocuments(VectorStoreConfig config) {
        EsResponse response = send(config, "GET", "/" + config.indexName() + "/_count", null, false);
        try {
            return objectMapper.readTree(response.body()).path("count").asLong();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse Elasticsearch count response", exception);
        }
    }

    private EsResponse send(
            VectorStoreConfig config,
            String method,
            String path,
            Object body,
            boolean allowNotFound
    ) {
        try {
            HttpURLConnection connection = openConnection(config, path);
            connection.setRequestMethod(method);
            connection.setConnectTimeout(config.connectTimeoutMs());
            connection.setReadTimeout(config.readTimeoutMs());
            connection.setRequestProperty("Content-Type", "application/json");
            applyAuthorization(config, connection);
            if (body != null) {
                connection.setDoOutput(true);
                byte[] payload = objectMapper.writeValueAsBytes(body);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payload);
                }
            }
            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection);
            EsResponse response = new EsResponse(statusCode, responseBody);
            if (statusCode == 404 && allowNotFound) {
                return response;
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("Elasticsearch request failed: " + statusCode + " " + responseBody);
            }
            return response;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to call Elasticsearch", exception);
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

    private HttpURLConnection openConnection(VectorStoreConfig config, String path) throws IOException {
        URL url = uri(config, path).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection instanceof HttpsURLConnection httpsConnection) {
            httpsConnection.setSSLSocketFactory(insecureSslContext().getSocketFactory());
            httpsConnection.setHostnameVerifier((hostname, session) -> true);
        }
        return connection;
    }

    private SSLContext insecureSslContext() {
        try {
            TrustManager[] trustManagers = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException exception) {
            throw new IllegalStateException("Unable to initialize Elasticsearch SSL context", exception);
        }
    }

    private void applyAuthorization(VectorStoreConfig config, HttpURLConnection connection) {
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            connection.setRequestProperty("Authorization", "ApiKey " + config.apiKey());
            return;
        }
        if ((config.username() != null && !config.username().isBlank()) || (config.password() != null && !config.password().isBlank())) {
            String credentials = (config.username() == null ? "" : config.username()) + ":" + (config.password() == null ? "" : config.password());
            connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private String readResponseBody(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getResponseCode() >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        if (inputStream == null) {
            return "";
        }
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record EsResponse(int statusCode, String body) {
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
