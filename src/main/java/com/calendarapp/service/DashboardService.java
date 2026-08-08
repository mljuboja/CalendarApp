package com.calendarapp.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.calendarapp.dto.DashboardResponse;
import com.calendarapp.dto.EventResponse;
import com.calendarapp.dto.TaskResponse;

// Combines existing Event and Task data into one summary response for the
// authenticated user. This is not a general analytics system - it just reuses
// EventService/TaskService for the underlying data and adds one small
// calculation (today's scheduled hours).
@Service
public class DashboardService {

    private final EventService eventService;
    private final TaskService taskService;

    public DashboardService(EventService eventService, TaskService taskService) {
        this.eventService = eventService;
        this.taskService = taskService;
    }

    public DashboardResponse getDashboard(Long ownerId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        // Reuses EventService's existing date-range filtering and recurrence
        // expansion - a recurring event's occurrence today is already included.
        List<EventResponse> todaysEvents = eventService.listEvents(ownerId, todayStart, tomorrowStart, null, null);
        List<TaskResponse> upcomingTasks = taskService.getUpcomingTasks(ownerId);
        long completedTaskCount = taskService.getCompletedTaskCount(ownerId);
        double scheduledHoursToday = calculateScheduledHours(todaysEvents, todayStart, tomorrowStart);

        return new DashboardResponse(todaysEvents, upcomingTasks, completedTaskCount, scheduledHoursToday);
    }

    // Sums the portion of each event that falls within [todayStart, tomorrowStart),
    // so an event crossing midnight only counts the part that's actually today.
    private static double calculateScheduledHours(
            List<EventResponse> todaysEvents, LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        long totalMinutes = 0;
        for (EventResponse event : todaysEvents) {
            LocalDateTime effectiveStart = event.getStartTime();
            if (effectiveStart.isBefore(todayStart)) {
                effectiveStart = todayStart;
            }

            LocalDateTime effectiveEnd = event.getEndTime();
            if (effectiveEnd.isAfter(tomorrowStart)) {
                effectiveEnd = tomorrowStart;
            }

            totalMinutes += Duration.between(effectiveStart, effectiveEnd).toMinutes();
        }
        return totalMinutes / 60.0;
    }
}
