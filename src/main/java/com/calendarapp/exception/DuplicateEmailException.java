package com.calendarapp.exception;

/**
 * Thrown when a registration request uses an email that already belongs to
 * an existing {@link com.calendarapp.entity.User}. Mapped to {@code 409 Conflict}
 * by {@link GlobalExceptionHandler}.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
