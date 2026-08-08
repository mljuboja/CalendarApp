package com.calendarapp.dto;

import jakarta.validation.constraints.NotNull;

import com.calendarapp.entity.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// What the client sends to PATCH /api/tasks/{id}/status - just the new status,
// so a quick "mark as done" action doesn't require resending the whole task.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
