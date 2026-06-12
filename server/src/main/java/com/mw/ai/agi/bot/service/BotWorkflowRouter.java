package com.mw.ai.agi.bot.service;

import com.mw.ai.agi.bot.domain.AiBot;
import com.mw.ai.agi.bot.domain.BotCapability;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BotWorkflowRouter {
    public String resolveWorkflowId(AiBot bot, List<BotCapability> capabilities, String userMessage) {
        List<BotCapability> workflows = capabilities.stream()
                .filter(item -> "WORKFLOW".equalsIgnoreCase(item.capabilityType()) && item.enabled())
                .filter(item -> item.capabilityId() != null && !item.capabilityId().isBlank())
                .toList();
        if (workflows.isEmpty()) {
            return bot.workflowId();
        }
        if (workflows.size() == 1) {
            return workflows.get(0).capabilityId();
        }
        BotCapability best = null;
        int bestScore = -1;
        for (BotCapability capability : workflows) {
            int score = scoreMessage(userMessage, capability.routingKeywords(), capability.capabilityCode());
            if (capability.primaryCapability()) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = capability;
            }
        }
        if (best != null && bestScore > 0) {
            return best.capabilityId();
        }
        return workflows.stream()
                .filter(BotCapability::primaryCapability)
                .findFirst()
                .or(() -> workflows.stream().findFirst())
                .map(BotCapability::capabilityId)
                .orElse(bot.workflowId());
    }

    private int scoreMessage(String message, String routingKeywords, String capabilityCode) {
        if (message == null || message.isBlank()) {
            return 0;
        }
        String lower = message.toLowerCase();
        int score = 0;
        if (capabilityCode != null && !capabilityCode.isBlank() && lower.contains(capabilityCode.toLowerCase())) {
            score += 2;
        }
        if (routingKeywords == null || routingKeywords.isBlank()) {
            return score;
        }
        for (String keyword : routingKeywords.split("[,，\\s]+")) {
            String trimmed = keyword.trim().toLowerCase();
            if (!trimmed.isEmpty() && lower.contains(trimmed)) {
                score += 1;
            }
        }
        return score;
    }
}
