package com.aiworkflow.common.exception;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
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
