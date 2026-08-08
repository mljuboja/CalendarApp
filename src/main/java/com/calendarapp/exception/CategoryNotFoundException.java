package com.calendarapp.exception;

// Thrown when a category doesn't exist, or exists but isn't owned by the requesting
// user. We use the same generic message and 404 status for both cases so we don't
// reveal that a category belonging to someone else exists.
// GlobalExceptionHandler turns this into a 404 Not Found response.
public class CategoryNotFoundException extends RuntimeException {

    private static final String GENERIC_MESSAGE = "Category not found";

    public CategoryNotFoundException() {
        super(GENERIC_MESSAGE);
    }
}
