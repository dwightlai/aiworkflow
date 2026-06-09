package com.mw.ai.agi.openapi.client.model;

import java.util.Map;

public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {
}
