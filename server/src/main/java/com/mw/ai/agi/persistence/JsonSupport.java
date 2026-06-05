package com.mw.ai.agi.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonSupport {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public JsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write JSON.", exception);
        }
    }

    public <T> T read(String value, Class<T> targetType) {
        try {
            return objectMapper.readValue(value, targetType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read JSON.", exception);
        }
    }

    public <T> T read(String value, TypeReference<T> targetType) {
        try {
            return objectMapper.readValue(value, targetType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read JSON.", exception);
        }
    }

    public Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        return read(value, MAP_TYPE);
    }
}
