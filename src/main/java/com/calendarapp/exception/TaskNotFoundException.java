package com.calendarapp.exception;

// Thrown when a task doesn't exist, or exists but isn't owned by the requesting
// user. We use the same generic message and 404 status for both cases so we don't
// reveal that a task belonging to someone else exists.
// GlobalExceptionHandler turns this into a 404 Not Found response.
public class TaskNotFoundException extends RuntimeException {

    private static final String GENERIC_MESSAGE = "Task not found";

    public TaskNotFoundException() {
        super(GENERIC_MESSAGE);
    }
}
