package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KnowledgeRetrievalFilters {
    private final List<String> materialSourceTypes;
    private final List<String> materialTypes;
    private final List<String> securityLevelAllowed;
    private final boolean archiveOnly;

    private KnowledgeRetrievalFilters(
            List<String> materialSourceTypes,
            List<String> materialTypes,
            List<String> securityLevelAllowed,
            boolean archiveOnly
    ) {
        this.materialSourceTypes = List.copyOf(materialSourceTypes);
        this.materialTypes = List.copyOf(materialTypes);
        this.securityLevelAllowed = List.copyOf(securityLevelAllowed);
        this.archiveOnly = archiveOnly;
    }

    public static KnowledgeRetrievalFilters empty() {
        return new KnowledgeRetrievalFilters(List.of(), List.of(), List.of(), false);
    }

    public static KnowledgeRetrievalFilters from(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return empty();
        }
        return new KnowledgeRetrievalFilters(
                readStringList(raw, "materialSourceType", "materialSourceTypes"),
                readStringList(raw, "materialType", "materialTypes"),
                readStringList(raw, "securityLevelAllowed", "securityLevels"),
                readBoolean(raw, "archiveOnly", "onlyArchiveMaterial")
        );
    }

    public boolean isEmpty() {
        return materialSourceTypes.isEmpty()
                && materialTypes.isEmpty()
                && securityLevelAllowed.isEmpty()
                && !archiveOnly;
    }

    public boolean matches(KnowledgeChunk chunk) {
        if (chunk == null || isEmpty()) {
            return chunk != null;
        }
        if (archiveOnly && isBlank(chunk.sourceArchiveFileId())) {
            return false;
        }
        if (!materialSourceTypes.isEmpty() && !containsIgnoreCase(materialSourceTypes, chunk.materialSourceType())) {
            return false;
        }
        if (!materialTypes.isEmpty() && !containsIgnoreCase(materialTypes, chunk.materialType())) {
            return false;
        }
        if (!securityLevelAllowed.isEmpty() && !containsIgnoreCase(securityLevelAllowed, chunk.securityLevel())) {
            return false;
        }
        return true;
    }

    private static List<String> readStringList(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value != null) {
                return normalizeStringList(value);
            }
        }
        return List.of();
    }

    private static List<String> normalizeStringList(Object value) {
        if (value instanceof Collection<?> collection) {
            List<String> items = new ArrayList<>();
            for (Object item : collection) {
                if (item != null) {
                    String normalized = String.valueOf(item).trim();
                    if (!normalized.isEmpty()) {
                        items.add(normalized);
                    }
                }
            }
            return items;
        }
        String single = String.valueOf(value).trim();
        if (single.isEmpty()) {
            return List.of();
        }
        if (single.contains(",")) {
            return List.of(single.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        return List.of(single);
    }

    private static boolean readBoolean(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return false;
    }

    private static boolean containsIgnoreCase(List<String> allowed, String value) {
        if (isBlank(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return allowed.stream()
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .anyMatch(item -> item.equals(normalized));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
