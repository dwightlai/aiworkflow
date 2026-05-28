package com.aiworkflow.common.exception;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.common.api.ErrorResponse;
import com.aiworkflow.workflow.engine.WorkflowRunNotFoundException;
import com.aiworkflow.workflow.service.DagValidationException;
import com.aiworkflow.workflow.service.WorkflowNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DagValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDagValidation(DagValidationException exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_WORKFLOW_DAG",
                exception.getMessage(),
                request.getRequestId(),
                null
        );
        return ApiResponse.failure(error);
    }

    @ExceptionHandler(WorkflowNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleWorkflowNotFound(WorkflowNotFoundException exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "WORKFLOW_NOT_FOUND",
                exception.getMessage(),
                request.getRequestId(),
                null
        );
        return ApiResponse.failure(error);
    }

    @ExceptionHandler(WorkflowRunNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleWorkflowRunNotFound(
            WorkflowRunNotFoundException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                "WORKFLOW_RUN_NOT_FOUND",
                exception.getMessage(),
                request.getRequestId(),
                null
        );
        return ApiResponse.failure(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed.",
                request.getRequestId(),
                Map.of("fieldErrors", exception.getBindingResult().getFieldErrorCount())
        );
        return ApiResponse.failure(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String errorCode = status != null ? status.name() : "HTTP_" + exception.getStatusCode().value();
        String message = exception.getReason() != null
                ? exception.getReason()
                : status != null ? status.getReasonPhrase() : errorCode;
        ErrorResponse error = new ErrorResponse(
                errorCode,
                message,
                request.getRequestId(),
                null
        );
        return ResponseEntity.status(exception.getStatusCode()).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "Unexpected server error.",
                request.getRequestId(),
                null
        );
        return ApiResponse.failure(error);
    }
}
