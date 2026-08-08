package com.calendarapp.exception;

// Thrown when an event doesn't exist, or exists but isn't owned (through its
// calendar) by the requesting user. We use the same generic message and 404
// status for both cases so we don't reveal that an event belonging to someone
// else exists.
// GlobalExceptionHandler turns this into a 404 Not Found response.
public class EventNotFoundException extends RuntimeException {

    private static final String GENERIC_MESSAGE = "Event not found";

    public EventNotFoundException() {
        super(GENERIC_MESSAGE);
    }
}
