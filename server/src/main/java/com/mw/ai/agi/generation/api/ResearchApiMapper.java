package com.mw.ai.agi.generation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.generation.domain.GenerationJob;
import com.mw.ai.agi.generation.domain.GenerationOutput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResearchApiMapper {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {
    };

    private ResearchApiMapper() {
    }

    public static ResearchJobView toJobView(GenerationJob job, ObjectMapper objectMapper) {
        Map<String, Object> variables = readMap(job.variables(), objectMapper);
        List<Map<String, Object>> outline = readOutline(job.outlineJson(), objectMapper);
        List<Map<String, Object>> sectionOutputs = readSectionOutputs(job.sectionOutputsJson(), objectMapper);
        Object workflowRunSnapshot = readJsonValue(job.workflowRunSnapshot(), objectMapper);
        return new ResearchJobView(
                job.id(),
                job.templateId(),
                job.status(),
                toStringMap(variables),
                readStringList(job.knowledgeBaseIds(), objectMapper),
                job.externalCorpusRef(),
                outline,
                sectionOutputs,
                workflowRunSnapshot,
                job.errorMessage(),
                job.startedAt(),
                job.completedAt(),
                job.createdAt()
        );
    }

    public static GenerationOutputView toOutputView(GenerationOutput output, ObjectMapper objectMapper) {
        return new GenerationOutputView(
                output.id(),
                output.jobId(),
                output.title(),
                output.outputType(),
                output.contentMarkdown(),
                output.contentDocxPath() != null && !output.contentDocxPath().isBlank(),
                readListMap(output.citations(), objectMapper),
                output.status(),
                output.createdAt()
        );
    }

    private static List<Map<String, Object>> readOutline(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            if (parsed instanceof List<?> items) {
                return toMapList(items);
            }
            if (parsed instanceof Map<?, ?> map) {
                Object sections = map.get("sections");
                if (sections instanceof List<?> sectionItems) {
                    List<Map<String, Object>> outline = new ArrayList<>();
                    for (Object item : sectionItems) {
                        if (item instanceof Map<?, ?> section) {
                            outline.add(Map.of(
                                    "key", String.valueOf(section.get("key")),
                                    "title", String.valueOf(section.get("title"))
                            ));
                        }
                    }
                    return outline;
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    private static List<Map<String, Object>> readSectionOutputs(String json, ObjectMapper objectMapper) {
        List<Map<String, Object>> outputs = readListMap(json, objectMapper);
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> output : outputs) {
            Map<String, Object> section = new LinkedHashMap<>(output);
            if (!section.containsKey("key") && section.containsKey("sectionKey")) {
                section.put("key", section.get("sectionKey"));
            }
            Object citations = section.get("citations");
            if (citations instanceof List<?> items) {
                section.put("citations", normalizeCitations(items));
            }
            normalized.add(section);
        }
        return normalized;
    }

    private static List<Map<String, Object>> normalizeCitations(List<?> items) {
        List<Map<String, Object>> citations = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> citation = new LinkedHashMap<>();
            map.forEach((key, value) -> citation.put(String.valueOf(key), value));
            if (!citation.containsKey("title")) {
                Object title = citation.get("citationText");
                if (title == null) {
                    title = citation.get("sourceId");
                }
                citation.put("title", String.valueOf(title));
            }
            citations.add(citation);
        }
        return citations;
    }

    private static Map<String, String> toStringMap(Map<String, Object> values) {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
        return result;
    }

    private static List<String> readStringList(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static Map<String, Object> readMap(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private static List<Map<String, Object>> readListMap(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_MAP_TYPE);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static Object readJsonValue(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception exception) {
            return json;
        }
    }

    private static List<Map<String, Object>> toMapList(List<?> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> entry = new LinkedHashMap<>();
                map.forEach((key, value) -> entry.put(String.valueOf(key), value));
                result.add(entry);
            }
        }
        return result;
    }

    public record ResearchJobView(
            String id,
            String templateId,
            String status,
            Map<String, String> variables,
            List<String> knowledgeBaseIds,
            String externalCorpusRef,
            List<Map<String, Object>> outline,
            List<Map<String, Object>> sectionOutputs,
            Object workflowRunSnapshot,
            String errorMessage,
            java.time.Instant startedAt,
            java.time.Instant completedAt,
            java.time.Instant createdAt
    ) {
    }

    public record GenerationOutputView(
            String id,
            String jobId,
            String title,
            String outputType,
            String contentMarkdown,
            boolean hasDocx,
            List<Map<String, Object>> citations,
            String status,
            java.time.Instant createdAt
    ) {
    }
}
