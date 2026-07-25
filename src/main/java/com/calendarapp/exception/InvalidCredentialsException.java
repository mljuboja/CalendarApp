package com.calendarapp.exception;

// Thrown when login fails, whether the email doesn't exist or the password is wrong.
// We use the same exception/message for both so we don't tell an attacker which one it was.
// GlobalExceptionHandler turns this into a 401 Unauthorized response.
public class InvalidCredentialsException extends RuntimeException {

    private static final String GENERIC_MESSAGE = "Invalid email or password";

    public InvalidCredentialsException() {
        super(GENERIC_MESSAGE);
    }
}
