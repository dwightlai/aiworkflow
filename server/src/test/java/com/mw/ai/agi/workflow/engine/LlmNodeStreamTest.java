package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.service.ChatModelClient;
import com.mw.ai.agi.model.service.ChatModelStreamConsumer;
import com.mw.ai.agi.model.service.InMemoryModelProviderStore;
import com.mw.ai.agi.model.service.ModelProviderService;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmNodeStreamTest {
    @Test
    void emitsRealDeltasWhenStreamContextIsActive() {
        List<String> deltas = new ArrayList<>();
        WorkflowStreamSink sink = new WorkflowStreamSink() {
            @Override
            public void emitLlmDelta(String delta) {
                deltas.add(delta);
            }

            @Override
            public void emitCitation(Map<String, Object> citation) {
            }
        };
        ChatModelClient chatModelClient = new ChatModelClient() {
            @Override
            public String generate(String providerId, String model, String prompt, Map<String, Object> options) {
                return "full-answer";
            }

            @Override
            public String generateStream(
                    String providerId,
                    String model,
                    String prompt,
                    Map<String, Object> options,
                    ChatModelStreamConsumer consumer
            ) {
                consumer.onDelta("full");
                consumer.onDelta("-answer");
                return "full-answer";
            }
        };
        InMemoryModelProviderStore store = new InMemoryModelProviderStore();
        store.save(new ModelProvider(
                "provider-1",
                "tenant_default",
                null,
                "gpt-test",
                "Test",
                "CHAT",
                null,
                false,
                BigDecimal.ONE,
                "http://localhost/v1",
                "gpt-test",
                "key",
                true,
                null,
                null,
                Instant.now(),
                Instant.now()
        ));
        ModelProviderService modelProviderService = new ModelProviderService(store);
        LlmNodeExecutor executor = new LlmNodeExecutor(chatModelClient, modelProviderService);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("providerId", "provider-1");
        config.put("promptKey", "question");
        WorkflowNode node = new WorkflowNode("llm-1", WorkflowNodeType.LLM, "llm", config);

        NodeExecutionResult result = WorkflowStreamContext.callWith(sink, () ->
                executor.execute(node, new NodeExecutionContext(Map.of("question", "hello"), Map.of("question", "hello")))
        );

        assertThat(result.output().get("content")).isEqualTo("full-answer");
        assertThat(deltas).containsExactly("full", "-answer");
    }
}
