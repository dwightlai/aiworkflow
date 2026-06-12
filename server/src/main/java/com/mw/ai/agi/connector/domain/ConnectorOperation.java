package com.mw.ai.agi.connector.domain;

import java.time.Instant;

public record ConnectorOperation(
        String id,
        String tenantId,
        String connectorId,
        String name,
        String code,
        String method,
        String path,
        String operationType,
        String riskLevel,
        boolean needConfirm,
        String confirmSummaryTemplate,
        String requestTemplate,
        boolean enabled,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
