package com.aiworkflow.workflow.engine;

import com.aiworkflow.model.service.ChatModelClient;
import com.aiworkflow.model.service.ModelProviderService;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiNodeExecutorTest {

    @Test
    void promptNodeRendersTemplateIntoContext() {
        PromptNodeExecutor executor = new PromptNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada")
        );

        NodeExecutionResult result = executor.execute(node(
                "prompt",
                WorkflowNodeType.PROMPT,
                Map.of("template", "Hello {{name}}", "outputKey", "prompt")
        ), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("prompt", "Hello Ada"));
    }

    @Test
    void llmNodeCallsModelClientAndStoresOutput() {
        RecordingChatModelClient chatModelClient = new RecordingChatModelClient();
        ModelProviderService modelProviderService = new ModelProviderService();
        String providerId = modelProviderService.create(
                "DeepSeek",
                "DeepSeek",
                "CHAT",
                null,
                false,
                BigDecimal.ONE,
                "https://api.deepseek.com/v1",
                "deepseek-chat",
                "dev-key",
                true
        ).id();
        LlmNodeExecutor executor = new LlmNodeExecutor(chatModelClient, modelProviderService);
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("prompt", "Hello Ada")
        );

        NodeExecutionResult result = executor.execute(node(
                "llm",
                WorkflowNodeType.LLM,
                Map.of(
                        "providerId", providerId,
                        "promptKey", "prompt",
                        "outputKey", "answer",
                        "temperature", 0.2
                )
        ), context);

        assertThat(chatModelClient.providerId).isEqualTo(providerId);
        assertThat(chatModelClient.model).isEqualTo("deepseek-chat");
        assertThat(chatModelClient.prompt).isEqualTo("Hello Ada");
        assertThat(result.output()).containsExactlyEntriesOf(Map.of("answer", "model response"));
    }

    @Test
    void llmNodeRejectsDisabledModelProviders() {
        RecordingChatModelClient chatModelClient = new RecordingChatModelClient();
        ModelProviderService modelProviderService = new ModelProviderService();
        String providerId = modelProviderService.create(
                "Disabled",
                "OpenAI",
                "CHAT",
                null,
                false,
                BigDecimal.ONE,
                "https://api.example.com/v1",
                "gpt-4.1-mini",
                "dev-key",
                false
        ).id();
        LlmNodeExecutor executor = new LlmNodeExecutor(chatModelClient, modelProviderService);

        assertThatThrownBy(() -> executor.execute(node(
                "llm",
                WorkflowNodeType.LLM,
                Map.of(
                        "providerId", providerId,
                        "promptKey", "prompt",
                        "outputKey", "answer"
                )
        ), new NodeExecutionContext(Map.of(), Map.of("prompt", "Hello"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Model provider is disabled: " + providerId);
    }

    private WorkflowNode node(String id, WorkflowNodeType type, Map<String, Object> config) {
        return new WorkflowNode(id, type, id, config);
    }

    private static class RecordingChatModelClient implements ChatModelClient {
        private String providerId;
        private String model;
        private String prompt;

        @Override
        public String generate(String providerId, String model, String prompt, Map<String, Object> options) {
            this.providerId = providerId;
            this.model = model;
            this.prompt = prompt;
            return "model response";
        }
    }
}
