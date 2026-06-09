package com.mw.ai.agi.openapi.client.model;

import java.util.List;

public record PageResponse<T>(List<T> items, long total) {
}
