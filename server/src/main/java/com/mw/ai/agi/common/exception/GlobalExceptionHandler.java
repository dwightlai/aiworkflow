package com.mw.ai.agi.common.exception;

import com.mw.ai.agi.bot.service.BotNotFoundException;
import com.mw.ai.agi.auth.service.AuthException;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.common.api.ErrorResponse;
import com.mw.ai.agi.workflow.engine.WorkflowRunNotFoundException;
import com.mw.ai.agi.workflow.service.DagValidationException;
import com.mw.ai.agi.workflow.service.WorkflowNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                exception.getCode(),
                exception.getMessage(),
                request.getRequestId(),
                null
        );
        return ResponseEntity.status(exception.getStatus()).body(ApiResponse.failure(error));
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

    @ExceptionHandler(BotNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleBotNotFound(BotNotFoundException exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "BOT_NOT_FOUND",
                exception.getMessage(),
                request.getRequestId(),
                null
        );
        return ApiResponse.failure(error);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(RuntimeException exception, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "BAD_REQUEST",
                exception.getMessage(),
                request.getRequestId(),
                null
        );
        return ApiResponse.failure(error);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "Unexpected server error.",
                request.getRequestId(),
                null
        );
        return ApiResponse.failure(error);
    }
}
