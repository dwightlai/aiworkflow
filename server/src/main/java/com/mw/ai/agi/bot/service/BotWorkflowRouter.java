package com.mw.ai.agi.bot.service;

import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotCapability;
import com.mw.ai.agi.bot.domain.WorkflowRoutePreview;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BotWorkflowRouter {
    private final WorkflowApplicationService workflowService;

    public BotWorkflowRouter(WorkflowApplicationService workflowService) {
        this.workflowService = workflowService;
    }

    public String resolveWorkflowId(AiBot bot, List<BotCapability> capabilities, String userMessage) {
        return previewRoute(bot, capabilities, userMessage).workflowId();
    }

    public WorkflowRoutePreview previewRoute(AiBot bot, List<BotCapability> capabilities, String userMessage) {
        List<BotCapability> workflows = capabilities.stream()
                .filter(item -> "WORKFLOW".equalsIgnoreCase(item.capabilityType()) && item.enabled())
                .filter(item -> item.capabilityId() != null && !item.capabilityId().isBlank())
                .toList();
        if (workflows.isEmpty()) {
            return toPreview(bot.workflowId(), null, "default");
        }
        if (workflows.size() == 1) {
            BotCapability only = workflows.get(0);
            return toPreview(only.capabilityId(), only.capabilityCode(), "single");
        }
        BotCapability best = null;
        int bestScore = -1;
        int bestBaseScore = -1;
        String bestReason = "primary";
        for (BotCapability capability : workflows) {
            ScoreResult score = scoreMessage(userMessage, capability.routingKeywords(), capability.capabilityCode());
            int base = score.score();
            int total = base;
            if (capability.primaryCapability()) {
                total += 1;
            }
            boolean replace = total > bestScore || (total == bestScore && base > bestBaseScore);
            if (replace) {
                bestScore = total;
                bestBaseScore = base;
                best = capability;
                bestReason = score.reason();
            }
        }
        if (best != null && bestScore > 0) {
            return toPreview(best.capabilityId(), best.capabilityCode(), bestReason);
        }
        BotCapability fallback = workflows.stream()
                .filter(BotCapability::primaryCapability)
                .findFirst()
                .or(() -> workflows.stream().findFirst())
                .orElse(null);
        if (fallback == null) {
            return toPreview(bot.workflowId(), null, "default");
        }
        return toPreview(fallback.capabilityId(), fallback.capabilityCode(), "primary");
    }

    private WorkflowRoutePreview toPreview(String workflowId, String capabilityCode, String matchReason) {
        String safeWorkflowId = workflowId == null ? "" : workflowId;
        String workflowName = "";
        if (!safeWorkflowId.isBlank()) {
            try {
                workflowName = workflowService.getWorkflow(safeWorkflowId).name();
            } catch (RuntimeException ignored) {
                workflowName = safeWorkflowId;
            }
        }
        return new WorkflowRoutePreview(safeWorkflowId, workflowName, capabilityCode, matchReason);
    }

    private ScoreResult scoreMessage(String message, String routingKeywords, String capabilityCode) {
        if (message == null || message.isBlank()) {
            return new ScoreResult(0, "empty");
        }
        String lower = message.toLowerCase();
        int score = 0;
        String reason = "primary";
        if (capabilityCode != null && !capabilityCode.isBlank() && lower.contains(capabilityCode.toLowerCase())) {
            score += 2;
            reason = "code:" + capabilityCode;
        }
        if (routingKeywords == null || routingKeywords.isBlank()) {
            return new ScoreResult(score, reason);
        }
        for (String keyword : routingKeywords.split("[,，\\s]+")) {
            String trimmed = keyword.trim().toLowerCase();
            if (!trimmed.isEmpty() && lower.contains(trimmed)) {
                score += 1;
                reason = "keyword:" + keyword.trim();
            }
        }
        return new ScoreResult(score, reason);
    }

    private record ScoreResult(int score, String reason) {
    }
}
