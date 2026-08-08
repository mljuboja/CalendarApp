package com.calendarapp.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.calendarapp.entity.RecurrenceType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// What the client sends to create or update an event. calendarId is required and
// must belong to the authenticated user; categoryId is optional but must also
// belong to the authenticated user if supplied. Both are checked in EventService,
// not here, since that requires the authenticated user and the database.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String location;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private boolean allDay;

    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    private Integer reminderOffsetMinutes;

    @NotNull(message = "Calendar is required")
    private Long calendarId;

    private Long categoryId;
}
