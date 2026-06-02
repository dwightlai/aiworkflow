package com.aiworkflow.workflow.service;

public class DagValidationException extends RuntimeException {
    public DagValidationException(String message) {
        super(message);
    }
}
