package com.calendarapp.exception;

/**
 * Thrown when login fails, for either an unknown email or an incorrect
 * password. Always carries the same generic message so the client-facing
 * response never reveals which of the two actually failed — this prevents
 * using login as an oracle to enumerate registered email addresses. Mapped
 * to {@code 401 Unauthorized} by {@link GlobalExceptionHandler}.
 */
public class InvalidCredentialsException extends RuntimeException {

    private static final String GENERIC_MESSAGE = "Invalid email or password";

    public InvalidCredentialsException() {
        super(GENERIC_MESSAGE);
    }
}
