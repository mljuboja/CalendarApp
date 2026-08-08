package com.calendarapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.calendarapp.dto.DashboardResponse;
import com.calendarapp.dto.EventResponse;
import com.calendarapp.dto.TaskResponse;
import com.calendarapp.entity.Priority;
import com.calendarapp.entity.RecurrenceType;
import com.calendarapp.entity.TaskStatus;

// Tests for DashboardService. EventService/TaskService are mocked - this only
// proves DashboardService combines their results correctly and calculates
// scheduled hours correctly. Event filtering/recurrence itself is already
// covered by EventServiceTest and is not repeated here.
class DashboardServiceTest {

    private EventService eventService;
    private TaskService taskService;
    private DashboardService dashboardService;

    private LocalDateTime todayStart;
    private LocalDateTime tomorrowStart;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        taskService = mock(TaskService.class);
        dashboardService = new DashboardService(eventService, taskService);

        todayStart = LocalDate.now().atStartOfDay();
        tomorrowStart = todayStart.plusDays(1);
    }

    @Test
    void dashboardIncludesTodaysEventsUpcomingTasksAndCompletedCount() {
        EventResponse event = eventWith(todayStart.plusHours(9), todayStart.plusHours(10));
        TaskResponse task = taskWith(TaskStatus.TODO);

        when(eventService.listEvents(1L, todayStart, tomorrowStart, null, null)).thenReturn(List.of(event));
        when(taskService.getUpcomingTasks(1L)).thenReturn(List.of(task));
        when(taskService.getCompletedTaskCount(1L)).thenReturn(3L);

        DashboardResponse response = dashboardService.getDashboard(1L);

        assertThat(response.getTodaysEvents()).containsExactly(event);
        assertThat(response.getUpcomingTasks()).containsExactly(task);
        assertThat(response.getCompletedTaskCount()).isEqualTo(3L);
    }

    @Test
    void scheduledHoursSumsFullyContainedEvents() {
        EventResponse morningEvent = eventWith(todayStart.plusHours(9), todayStart.plusHours(10));
        EventResponse afternoonEvent = eventWith(todayStart.plusHours(13), todayStart.plusHours(14).plusMinutes(30));

        when(eventService.listEvents(1L, todayStart, tomorrowStart, null, null))
                .thenReturn(List.of(morningEvent, afternoonEvent));
        when(taskService.getUpcomingTasks(1L)).thenReturn(List.of());
        when(taskService.getCompletedTaskCount(1L)).thenReturn(0L);

        DashboardResponse response = dashboardService.getDashboard(1L);

        assertThat(response.getScheduledHoursToday()).isEqualTo(2.5);
    }

    @Test
    void scheduledHoursClipsEventsCrossingDayBoundary() {
        // 11 PM yesterday -> 1 AM today: only the 1 hour inside today should count.
        EventResponse crossesIntoToday = eventWith(todayStart.minusHours(1), todayStart.plusHours(1));
        // 11 PM today -> 1 AM tomorrow: only the 1 hour inside today should count.
        EventResponse crossesIntoTomorrow = eventWith(todayStart.plusHours(23), tomorrowStart.plusHours(1));

        when(eventService.listEvents(1L, todayStart, tomorrowStart, null, null))
                .thenReturn(List.of(crossesIntoToday, crossesIntoTomorrow));
        when(taskService.getUpcomingTasks(1L)).thenReturn(List.of());
        when(taskService.getCompletedTaskCount(1L)).thenReturn(0L);

        DashboardResponse response = dashboardService.getDashboard(1L);

        assertThat(response.getScheduledHoursToday()).isEqualTo(2.0);
    }

    private static EventResponse eventWith(LocalDateTime start, LocalDateTime end) {
        return new EventResponse(
                1L, "Standup", null, null, start, end, false, RecurrenceType.NONE, null, 2L, "Work", null, null, null);
    }

    private static TaskResponse taskWith(TaskStatus status) {
        return new TaskResponse(1L, "Write report", null, LocalDate.now(), Priority.HIGH, status);
    }
}
