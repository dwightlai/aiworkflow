package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.model.service.ChatModelClient;
import com.mw.ai.agi.model.service.ModelProviderService;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
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
        assertThat(result.output()).containsEntry("answer", "model response");
        assertThat(result.output()).containsEntry("content", "model response");
        assertThat(result.output()).containsEntry("reasoning_content", "");
    }

    @Test
    void llmNodeRendersAiflowyStyleUserPromptAndOutputParam() {
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

        NodeExecutionResult result = executor.execute(node(
                "llm",
                WorkflowNodeType.LLM,
                Map.of(
                        "providerId", providerId,
                        "userPrompt", "请回答：{{question}}",
                        "systemPrompt", "你是客服助手",
                        "inputParams", List.of(Map.of("name", "question", "value", "userQuestion", "type", "String")),
                        "outputParams", List.of(Map.of("name", "output", "type", "String")),
                        "temperature", 0.5,
                        "topP", 0.9,
                        "topK", 50
                )
        ), new NodeExecutionContext(Map.of(), Map.of("userQuestion", "如何退款")));

        assertThat(chatModelClient.prompt).isEqualTo("你是客服助手\n\n请回答：如何退款");
        assertThat(chatModelClient.options).containsEntry("topK", 50);
        assertThat(result.output()).containsEntry("content", "model response");
        assertThat(result.output()).containsEntry("reasoning_content", "");
    }

    @Test
    void llmNodeSupportsMessageConfigResponseFormatAndStreamingOptions() {
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

        NodeExecutionResult result = executor.execute(node(
                "llm",
                WorkflowNodeType.LLM,
                Map.of(
                        "providerId", providerId,
                        "systemMessage", "You are a support assistant",
                        "userMessage", "Question: ${input}",
                        "inputParams", List.of(Map.of("name", "input", "value", "start.input", "type", "String")),
                        "responseFormat", "JSON",
                        "streaming", true,
                        "outputKey", "legacyAnswer"
                )
        ), new NodeExecutionContext(Map.of(), Map.of("start", Map.of("input", "How to refund?"))));

        assertThat(chatModelClient.prompt).isEqualTo("You are a support assistant\n\nQuestion: How to refund?");
        assertThat(chatModelClient.options).containsEntry("responseFormat", "JSON");
        assertThat(chatModelClient.options).containsEntry("streaming", true);
        assertThat(result.output()).containsEntry("content", "model response");
        assertThat(result.output()).containsEntry("reasoning_content", "");
        assertThat(result.output()).containsEntry("legacyAnswer", "model response");
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
        private Map<String, Object> options;

        @Override
        public String generate(String providerId, String model, String prompt, Map<String, Object> options) {
            this.providerId = providerId;
            this.model = model;
            this.prompt = prompt;
            this.options = options;
            return "model response";
        }
    }
}
