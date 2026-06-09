package com.mw.ai.agi.openapi.client;

import com.mw.ai.agi.openapi.client.model.ApiResponse;

public final class AgiOpenApiSupport {
    private AgiOpenApiSupport() {
    }

    public static <T> T requireData(ApiResponse<T> response) {
        if (response == null) {
            throw new AgiOpenApiException("EMPTY_RESPONSE", "AGI open API returned empty response");
        }
        if (!response.success()) {
            if (response.error() != null) {
                throw new AgiOpenApiException(response.error().code(), response.error().message());
            }
            throw new AgiOpenApiException("API_FAILED", "AGI open API call failed");
        }
        return response.data();
    }
}
