package com.mw.ai.agi.openapi.client;

public class AgiOpenApiException extends RuntimeException {
    private final String code;

    public AgiOpenApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
