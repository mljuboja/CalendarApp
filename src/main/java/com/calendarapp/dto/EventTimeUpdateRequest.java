package com.calendarapp.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// What the client sends to PATCH /api/events/{id}/time - just the two fields a
// drag/resize action on the calendar actually changes. Everything else about the
// event (title, calendar, category, recurrenceType, etc.) is left untouched.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventTimeUpdateRequest {

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}
