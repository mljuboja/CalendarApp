package com.calendarapp.exception;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

// The JSON shape we always return when something goes wrong. No stack traces or
// other internal details - just a status code, a message, and (for validation
// errors) which fields were invalid.
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
