package com.mw.ai.agi.asset.domain;

import java.time.Instant;

public record AssetGrant(
        String id,
        String assetType,
        String assetId,
        String permission,
        String unitId,
        String unitScope,
        String departmentId,
        String departmentScope,
        boolean enabled,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
