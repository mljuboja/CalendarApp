package com.calendarapp.exception;

// Thrown when an event's end time is not after its start time.
// GlobalExceptionHandler turns this into a 400 Bad Request response.
public class InvalidEventTimeException extends RuntimeException {

    public InvalidEventTimeException(String message) {
        super(message);
    }
}
