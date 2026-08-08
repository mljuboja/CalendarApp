package com.calendarapp.dto;

import java.time.LocalDate;

import com.calendarapp.entity.Priority;
import com.calendarapp.entity.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

// What we send back for a task. No owner/User info or timestamps included.
@Getter
@AllArgsConstructor
public class TaskResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final LocalDate dueDate;
    private final Priority priority;
    private final TaskStatus status;
}
