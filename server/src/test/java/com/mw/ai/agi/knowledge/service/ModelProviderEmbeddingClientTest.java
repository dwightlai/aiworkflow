package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.service.InMemoryModelProviderStore;
import com.mw.ai.agi.model.service.ModelProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ModelProviderEmbeddingClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void readsEmbeddingFromConfiguredOllamaProvider() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        StringBuilder requestBody = new StringBuilder();
        server.createContext("/api/embeddings", exchange -> {
            requestBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"embedding\":[0.1,0.2,0.3]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        InMemoryModelProviderStore store = new InMemoryModelProviderStore();
        store.save(new ModelProvider(
                "provider-1",
                "tenant_default",
                null,
                "ollama",
                "Ollama",
                "EMBEDDING",
                null,
                false,
                BigDecimal.ZERO,
                "http://localhost:" + server.getAddress().getPort(),
                "test-embedding",
                null,
                true,
                null,
                null,
                Instant.now(),
                Instant.now()
        ));
        ModelProviderEmbeddingClient client = new ModelProviderEmbeddingClient(
                new ModelProviderService(store),
                new ObjectMapper()
        );

        List<Double> embedding = client.embed("provider-1", "ignored", "hello");

        assertThat(embedding).containsExactly(0.1, 0.2, 0.3);
        assertThat(requestBody).contains("\"model\":\"test-embedding\"");
        assertThat(requestBody).contains("\"prompt\":\"hello\"");
    }

    @Test
    void readsBatchEmbeddingsFromConfiguredOllamaProvider() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        StringBuilder requestBody = new StringBuilder();
        server.createContext("/api/embed", exchange -> {
            requestBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"embeddings\":[[0.1,0.2],[0.3,0.4]]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ModelProviderEmbeddingClient client = new ModelProviderEmbeddingClient(
                new ModelProviderService(providerStore()),
                new ObjectMapper()
        );

        List<List<Double>> embeddings = client.embedAll("provider-1", "ignored", List.of("hello", "world"));

        assertThat(embeddings).containsExactly(List.of(0.1, 0.2), List.of(0.3, 0.4));
        assertThat(requestBody).contains("\"input\":[\"hello\",\"world\"]");
    }

    @Test
    void readsEmbeddingFromCustomOpenAiCompatibleEndpoint() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        StringBuilder requestBody = new StringBuilder();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> protocolUpgrade = new AtomicReference<>();
        server.createContext("/v1/embeddings", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            protocolUpgrade.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            requestBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"data":[{"index":0,"embedding":[0.11,0.22,0.33]}],"model":"Qwen3-VL-Embedding-2B"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ModelProviderEmbeddingClient client = new ModelProviderEmbeddingClient(
                new ModelProviderService(customProviderStore()),
                new ObjectMapper()
        );

        List<Double> embedding = client.embed("custom-provider", "ignored", "档案标准规范");

        assertThat(embedding).containsExactly(0.11, 0.22, 0.33);
        assertThat(requestBody).contains("\"model\":\"Qwen3-VL-Embedding-2B\"");
        assertThat(requestBody).contains("\"input\":\"档案标准规范\"");
        assertThat(authorization.get()).isEqualTo("Bearer secret-key");
        assertThat(protocolUpgrade.get()).isNull();
    }

    @Test
    void readsBatchEmbeddingsFromCustomOpenAiCompatibleEndpoint() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        StringBuilder requestBody = new StringBuilder();
        server.createContext("/v1/embeddings", exchange -> {
            requestBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"data":[
                      {"index":0,"embedding":[0.1,0.2]},
                      {"index":1,"embedding":[0.3,0.4]}
                    ]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ModelProviderEmbeddingClient client = new ModelProviderEmbeddingClient(
                new ModelProviderService(customProviderStore()),
                new ObjectMapper()
        );

        List<List<Double>> embeddings = client.embedAll(
                "custom-provider",
                "ignored",
                List.of("档案标准", "电子文件")
        );

        assertThat(embeddings).containsExactly(List.of(0.1, 0.2), List.of(0.3, 0.4));
        assertThat(requestBody).contains("\"input\":[\"档案标准\",\"电子文件\"]");
    }

    private InMemoryModelProviderStore providerStore() {
        InMemoryModelProviderStore store = new InMemoryModelProviderStore();
        store.save(new ModelProvider(
                "provider-1",
                "tenant_default",
                null,
                "ollama",
                "Ollama",
                "EMBEDDING",
                null,
                false,
                BigDecimal.ZERO,
                "http://localhost:" + server.getAddress().getPort(),
                "test-embedding",
                null,
                true,
                null,
                null,
                Instant.now(),
                Instant.now()
        ));
        return store;
    }

    private InMemoryModelProviderStore customProviderStore() {
        InMemoryModelProviderStore store = new InMemoryModelProviderStore();
        store.save(new ModelProvider(
                "custom-provider",
                "tenant_default",
                null,
                "Qwen3 VL embedding",
                "Custom",
                "EMBEDDING",
                null,
                true,
                BigDecimal.ZERO,
                "http://localhost:" + server.getAddress().getPort() + "/v1/embeddings",
                "Qwen3-VL-Embedding-2B",
                "secret-key",
                true,
                null,
                null,
                Instant.now(),
                Instant.now()
        ));
        return store;
    }
}
