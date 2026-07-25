package com.calendarapp.exception;

// Thrown when someone tries to register with an email that's already in use.
// GlobalExceptionHandler turns this into a 409 Conflict response.
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
