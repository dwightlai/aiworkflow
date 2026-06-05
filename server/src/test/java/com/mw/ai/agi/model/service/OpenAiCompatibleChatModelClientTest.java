package com.mw.ai.agi.model.service;

import com.mw.ai.agi.model.domain.ModelProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleChatModelClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void callsOpenAiCompatibleChatCompletionEndpoint() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        CapturedRequest captured = new CapturedRequest();
        server.createContext("/v1/chat/completions", exchange -> {
            captured.authorization = exchange.getRequestHeaders().getFirst("Authorization");
            captured.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "real model answer"
                          }
                        }
                      ]
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        InMemoryModelProviderStore store = new InMemoryModelProviderStore();
        store.save(new ModelProvider(
                "provider-1",
                "deepseek-chat",
                "DeepSeek",
                "CHAT",
                null,
                false,
                BigDecimal.ONE,
                "http://localhost:" + server.getAddress().getPort() + "/v1",
                "deepseek-chat",
                "secret-key",
                true,
                Instant.now(),
                Instant.now()
        ));
        OpenAiCompatibleChatModelClient client = new OpenAiCompatibleChatModelClient(
                new ModelProviderService(store),
                new ObjectMapper(),
                HttpClient.newHttpClient()
        );

        String answer = client.generate("provider-1", "ignored-model", "hello", Map.of("temperature", 0.2));

        assertThat(answer).isEqualTo("real model answer");
        assertThat(captured.authorization).isEqualTo("Bearer secret-key");
        assertThat(captured.body).contains("\"model\":\"deepseek-chat\"");
        assertThat(captured.body).contains("\"content\":\"hello\"");
        assertThat(captured.body).contains("\"temperature\":0.2");
    }

    private static class CapturedRequest {
        String authorization;
        String body;
    }
}
