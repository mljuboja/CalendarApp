package com.calendarapp.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// What we send back for the dashboard - a small summary combining today's
// Events with this user's Tasks. No new entities or analytics - just existing
// EventResponse/TaskResponse data plus two simple numbers.
@Getter
@AllArgsConstructor
public class DashboardResponse {

    private final List<EventResponse> todaysEvents;
    private final List<TaskResponse> upcomingTasks;
    private final long completedTaskCount;
    private final double scheduledHoursToday;
}
