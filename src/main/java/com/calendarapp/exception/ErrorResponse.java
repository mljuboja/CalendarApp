package com.calendarapp.exception;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

/**
 * Consistent JSON error body returned by {@link GlobalExceptionHandler} for
 * every handled exception. Never carries stack traces or other internal
 * exception details — only a status, a human-readable message, and (for
 * validation failures) per-field details.
 */
@Getter
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final List<FieldErrorDetail> fieldErrors;

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, List.of());
    }

    public ErrorResponse(int status, String error, String message, List<FieldErrorDetail> fieldErrors) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    @Getter
    public static class FieldErrorDetail {
        private final String field;
        private final String message;

        public FieldErrorDetail(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
