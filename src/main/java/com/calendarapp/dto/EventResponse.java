package com.calendarapp.dto;

import java.time.LocalDateTime;

import com.calendarapp.entity.RecurrenceType;

import lombok.AllArgsConstructor;
import lombok.Getter;

// What we send back for an event. No Calendar/Category/User entities included -
// just the simple related fields the frontend needs. categoryId/categoryName/
// categoryColor are null when the event has no category.
@Getter
@AllArgsConstructor
public class EventResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final String location;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final boolean allDay;
    private final RecurrenceType recurrenceType;
    private final Integer reminderOffsetMinutes;
    private final Long calendarId;
    private final String calendarName;
    private final Long categoryId;
    private final String categoryName;
    private final String categoryColor;
}
