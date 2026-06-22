package com.mw.ai.agi.bot.service;

import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotCapability;
import com.mw.ai.agi.bot.domain.BotStatus;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowStatus;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BotWorkflowRouterTest {
    private final WorkflowApplicationService workflowService = mock(WorkflowApplicationService.class);
    private BotWorkflowRouter router;

    @BeforeEach
    void setUp() {
        router = new BotWorkflowRouter(workflowService);
        when(workflowService.getWorkflow(anyString())).thenAnswer(invocation -> new Workflow(
                invocation.getArgument(0),
                "tenant_default",
                null,
                "Workflow " + invocation.getArgument(0),
                null,
                WorkflowStatus.PUBLISHED,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        ));
    }

    @Test
    void shouldUsePrimaryWorkflowWhenNoKeywordMatch() {
        AiBot bot = bot("bot_1", "wf_primary");
        List<BotCapability> capabilities = List.of(
                capability("wf_primary", "primary", "档案,规范", true),
                capability("wf_research", "research", "编研,报告", false)
        );
        assertThat(router.resolveWorkflowId(bot, capabilities, "你好")).isEqualTo("wf_primary");
    }

    @Test
    void shouldRouteByKeywords() {
        AiBot bot = bot("bot_1", "wf_primary");
        List<BotCapability> capabilities = List.of(
                capability("wf_primary", "primary", "档案,规范", true),
                capability("wf_research", "research", "编研,报告", false)
        );
        assertThat(router.resolveWorkflowId(bot, capabilities, "请帮我生成编研报告")).isEqualTo("wf_research");
    }

    private AiBot bot(String id, String workflowId) {
        return new AiBot(
                id, "tenant_default", "test", null, null, "robot", workflowId, null,
                List.of(), "", "", null, List.of(), BotStatus.ENABLED, 0,
                null, null, null, Instant.now(), Instant.now()
        );
    }

    private BotCapability capability(String workflowId, String code, String keywords, boolean primary) {
        return new BotCapability(
                "cap_" + workflowId,
                "tenant_default",
                "bot_1",
                "WORKFLOW",
                workflowId,
                code,
                keywords,
                primary,
                true,
                Instant.now(),
                Instant.now()
        );
    }
}
