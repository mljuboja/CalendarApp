package com.calendarapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.calendarapp.dto.CalendarRequest;
import com.calendarapp.dto.CalendarResponse;
import com.calendarapp.entity.Calendar;
import com.calendarapp.entity.User;
import com.calendarapp.exception.CalendarNotFoundException;
import com.calendarapp.repository.CalendarRepository;

// Tests for CalendarService. CalendarRepository is mocked so these don't need a
// real database - ownership is enforced by scoping every lookup by owner ID.
class CalendarServiceTest {

    private CalendarRepository calendarRepository;
    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarRepository = mock(CalendarRepository.class);
        calendarService = new CalendarService(calendarRepository);
    }

    @Test
    void createCalendarAssignsAuthenticatedUserAsOwner() {
        User owner = new User();
        owner.setId(1L);
        CalendarRequest request = new CalendarRequest("Work", "#4A90E2");

        when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> {
            Calendar calendar = invocation.getArgument(0);
            calendar.setId(10L);
            return calendar;
        });

        CalendarResponse response = calendarService.createCalendar(request, owner);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Work");
        assertThat(response.getColor()).isEqualTo("#4A90E2");

        ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
        verify(calendarRepository).save(calendarCaptor.capture());
        assertThat(calendarCaptor.getValue().getOwner()).isEqualTo(owner);
    }

    @Test
    void listCalendarsReturnsOnlyCalendarsForThatUser() {
        Calendar calendar1 = calendarWith(1L, "Work", "#4A90E2");
        Calendar calendar2 = calendarWith(2L, "Personal", "#FF5733");

        when(calendarRepository.findByOwnerId(1L)).thenReturn(List.of(calendar1, calendar2));

        List<CalendarResponse> responses = calendarService.listCalendars(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CalendarResponse::getName).containsExactly("Work", "Personal");
        verify(calendarRepository).findByOwnerId(1L);
    }

    @Test
    void getOwnedCalendarSucceeds() {
        Calendar calendar = calendarWith(5L, "Work", "#4A90E2");
        when(calendarRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(calendar));

        CalendarResponse response = calendarService.getCalendar(5L, 1L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Work");
    }

    @Test
    void getAnotherUsersCalendarThrowsCalendarNotFoundException() {
        when(calendarRepository.findByIdAndOwnerId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.getCalendar(5L, 2L))
                .isInstanceOf(CalendarNotFoundException.class);
    }

    @Test
    void updateCalendarChangesNameAndColor() {
        Calendar calendar = calendarWith(5L, "Work", "#4A90E2");
        CalendarRequest request = new CalendarRequest("Renamed", "#00FF00");

        when(calendarRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(calendar));
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CalendarResponse response = calendarService.updateCalendar(5L, request, 1L);

        assertThat(response.getName()).isEqualTo("Renamed");
        assertThat(response.getColor()).isEqualTo("#00FF00");
    }

    @Test
    void deleteOwnedCalendarCallsRepositoryDelete() {
        Calendar calendar = calendarWith(5L, "Work", "#4A90E2");
        when(calendarRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(calendar));

        calendarService.deleteCalendar(5L, 1L);

        verify(calendarRepository).delete(calendar);
    }

    private static Calendar calendarWith(Long id, String name, String color) {
        Calendar calendar = new Calendar();
        calendar.setId(id);
        calendar.setName(name);
        calendar.setColor(color);
        return calendar;
    }
}
