package com.aiworkflow.workflow.engine;

import com.aiworkflow.model.service.ChatModelClient;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        LlmNodeExecutor executor = new LlmNodeExecutor(chatModelClient);
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("prompt", "Hello Ada")
        );

        NodeExecutionResult result = executor.execute(node(
                "llm",
                WorkflowNodeType.LLM,
                Map.of(
                        "providerId", "dev",
                        "model", "mock",
                        "promptKey", "prompt",
                        "outputKey", "answer",
                        "temperature", 0.2
                )
        ), context);

        assertThat(chatModelClient.providerId).isEqualTo("dev");
        assertThat(chatModelClient.model).isEqualTo("mock");
        assertThat(chatModelClient.prompt).isEqualTo("Hello Ada");
        assertThat(result.output()).containsExactlyEntriesOf(Map.of("answer", "model response"));
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
