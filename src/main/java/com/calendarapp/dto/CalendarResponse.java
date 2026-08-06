package com.calendarapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// What we send back for a calendar. No owner/user info included.
@Getter
@AllArgsConstructor
public class CalendarResponse {

    private final Long id;
    private final String name;
    private final String color;
}
