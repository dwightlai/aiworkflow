package com.mw.ai.agi.common.asset;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AssetReferenceSupport {
    public static final int MAX_ASSET_ID_LENGTH = 128;

    private AssetReferenceSupport() {
    }

    public static String requireAssetId(String assetId) {
        String normalized = assetId == null ? "" : assetId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Asset id is required.");
        }
        if (normalized.length() > MAX_ASSET_ID_LENGTH) {
            throw new IllegalArgumentException("Asset id exceeds max length " + MAX_ASSET_ID_LENGTH + ".");
        }
        return normalized;
    }

    public static List<String> requireAssetIds(List<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String assetId : assetIds) {
            if (assetId != null && !assetId.isBlank()) {
                normalized.add(requireAssetId(assetId));
            }
        }
        return new ArrayList<>(normalized);
    }
}
