package com.calendarapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.calendarapp.dto.EventRequest;
import com.calendarapp.dto.EventResponse;
import com.calendarapp.entity.Calendar;
import com.calendarapp.entity.Category;
import com.calendarapp.entity.Event;
import com.calendarapp.entity.RecurrenceType;
import com.calendarapp.entity.User;
import com.calendarapp.exception.CalendarNotFoundException;
import com.calendarapp.exception.CategoryNotFoundException;
import com.calendarapp.exception.EventNotFoundException;
import com.calendarapp.exception.InvalidEventTimeException;
import com.calendarapp.repository.CalendarRepository;
import com.calendarapp.repository.CategoryRepository;
import com.calendarapp.repository.EventRepository;

// Tests for EventService. All repositories are mocked so these don't need a real
// database - ownership is enforced by scoping every lookup by owner ID, either
// directly (Calendar/Category) or through the event's calendar (Event itself).
class EventServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 1, 1, 10, 0);

    private EventRepository eventRepository;
    private CalendarRepository calendarRepository;
    private CategoryRepository categoryRepository;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        calendarRepository = mock(CalendarRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        eventService = new EventService(eventRepository, calendarRepository, categoryRepository);
    }

    @Test
    void createEventWithOwnedCalendarSucceeds() {
        User owner = userWith(1L);
        Calendar calendar = calendarWith(2L, owner);
        EventRequest request = eventRequest(2L, null);

        when(calendarRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(calendar));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(10L);
            return event;
        });

        EventResponse response = eventService.createEvent(request, owner);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Standup");
        assertThat(response.getCalendarId()).isEqualTo(2L);
        assertThat(response.getCategoryId()).isNull();
    }

    @Test
    void createEventWithAnotherUsersCalendarFails() {
        User owner = userWith(1L);
        EventRequest request = eventRequest(99L, null);

        when(calendarRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(request, owner))
                .isInstanceOf(CalendarNotFoundException.class);
    }

    @Test
    void createEventWithOwnedCategorySucceeds() {
        User owner = userWith(1L);
        Calendar calendar = calendarWith(2L, owner);
        Category category = categoryWith(3L, owner);
        EventRequest request = eventRequest(2L, 3L);

        when(calendarRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(calendar));
        when(categoryRepository.findByIdAndOwnerId(3L, 1L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.createEvent(request, owner);

        assertThat(response.getCategoryId()).isEqualTo(3L);
        assertThat(response.getCategoryName()).isEqualTo("Work");
        assertThat(response.getCategoryColor()).isEqualTo("#4A90E2");
    }

    @Test
    void createEventWithAnotherUsersCategoryFails() {
        User owner = userWith(1L);
        Calendar calendar = calendarWith(2L, owner);
        EventRequest request = eventRequest(2L, 99L);

        when(calendarRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(calendar));
        when(categoryRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(request, owner))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void endTimeNotAfterStartTimeIsRejected() {
        User owner = userWith(1L);
        Calendar calendar = calendarWith(2L, owner);
        when(calendarRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(calendar));

        EventRequest sameTime = eventRequest(2L, null);
        sameTime.setEndTime(sameTime.getStartTime());
        assertThatThrownBy(() -> eventService.createEvent(sameTime, owner))
                .isInstanceOf(InvalidEventTimeException.class);

        EventRequest endBeforeStart = eventRequest(2L, null);
        endBeforeStart.setEndTime(endBeforeStart.getStartTime().minusHours(1));
        assertThatThrownBy(() -> eventService.createEvent(endBeforeStart, owner))
                .isInstanceOf(InvalidEventTimeException.class);
    }

    @Test
    void listEventsWithNoFiltersReturnsAllEventsForThatUser() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event1 = eventWith(1L, "Standup", calendar, null);
        Event event2 = eventWith(2L, "Retro", calendar, null);

        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, null, null))
                .thenReturn(List.of(event1, event2));

        List<EventResponse> responses = eventService.listEvents(1L, null, null, null, null);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(EventResponse::getTitle).containsExactly("Standup", "Retro");
        verify(eventRepository).findByCalendarOwnerIdAndFilters(1L, null, null, null, null);
    }

    @Test
    void listEventsWithDateRangeFiltersByOverlap() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event overlapping = eventWith(1L, "Standup", calendar, null);
        LocalDateTime rangeStart = START.plusMinutes(30);
        LocalDateTime rangeEnd = END.plusHours(1);

        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, rangeStart, rangeEnd))
                .thenReturn(List.of(overlapping));

        List<EventResponse> responses = eventService.listEvents(1L, rangeStart, rangeEnd, null, null);

        assertThat(responses).extracting(EventResponse::getTitle).containsExactly("Standup");
    }

    @Test
    void listEventsWithOnlyOneDateParameterIsRejected() {
        assertThatThrownBy(() -> eventService.listEvents(1L, START, null, null, null))
                .isInstanceOf(InvalidEventTimeException.class);
        assertThatThrownBy(() -> eventService.listEvents(1L, null, END, null, null))
                .isInstanceOf(InvalidEventTimeException.class);
    }

    @Test
    void listEventsWithInvalidDateRangeIsRejected() {
        assertThatThrownBy(() -> eventService.listEvents(1L, END, START, null, null))
                .isInstanceOf(InvalidEventTimeException.class);
    }

    @Test
    void listEventsWithCalendarFilterVerifiesOwnership() {
        when(calendarRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.listEvents(1L, null, null, 99L, null))
                .isInstanceOf(CalendarNotFoundException.class);
    }

    @Test
    void listEventsWithCategoryFilterVerifiesOwnership() {
        when(categoryRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.listEvents(1L, null, null, null, 99L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void listEventsWithNoneRecurrenceReturnsOneOverlappingOccurrence() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event = eventWith(1L, "Standup", calendar, null);
        event.setRecurrenceType(RecurrenceType.NONE);
        LocalDateTime rangeStart = START.minusMinutes(30);
        LocalDateTime rangeEnd = END.plusMinutes(30);

        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, rangeStart, rangeEnd))
                .thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents(1L, rangeStart, rangeEnd, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStartTime()).isEqualTo(START);
        assertThat(responses.get(0).getEndTime()).isEqualTo(END);
    }

    @Test
    void dailyRecurrenceGeneratesExpectedOccurrencesWithinRange() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event = recurringEventWith(1L, calendar, RecurrenceType.DAILY);

        LocalDateTime rangeStart = START;
        LocalDateTime rangeEnd = START.plusDays(3);
        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, rangeStart, rangeEnd))
                .thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents(1L, rangeStart, rangeEnd, null, null);

        assertThat(responses).extracting(EventResponse::getStartTime)
                .containsExactly(START, START.plusDays(1), START.plusDays(2));
        assertThat(responses).allSatisfy(response -> assertThat(response.getId()).isEqualTo(1L));
    }

    @Test
    void weeklyRecurrenceGeneratesExpectedOccurrencesWithinRange() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event = recurringEventWith(1L, calendar, RecurrenceType.WEEKLY);

        LocalDateTime rangeStart = START;
        LocalDateTime rangeEnd = START.plusWeeks(2);
        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, rangeStart, rangeEnd))
                .thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents(1L, rangeStart, rangeEnd, null, null);

        assertThat(responses).extracting(EventResponse::getStartTime)
                .containsExactly(START, START.plusWeeks(1));
    }

    @Test
    void monthlyRecurrenceGeneratesExpectedOccurrencesWithinRangeAndPreservesDuration() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event = recurringEventWith(1L, calendar, RecurrenceType.MONTHLY);

        LocalDateTime rangeStart = START;
        LocalDateTime rangeEnd = START.plusMonths(2);
        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, rangeStart, rangeEnd))
                .thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents(1L, rangeStart, rangeEnd, null, null);

        assertThat(responses).extracting(EventResponse::getStartTime)
                .containsExactly(START, START.plusMonths(1));
        assertThat(responses).allSatisfy(response ->
                assertThat(Duration.between(response.getStartTime(), response.getEndTime()))
                        .isEqualTo(Duration.between(START, END)));
    }

    @Test
    void occurrencesOutsideRequestedRangeAreExcluded() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event = recurringEventWith(1L, calendar, RecurrenceType.DAILY);

        // Only the second daily occurrence (START.plusDays(1)) overlaps this window.
        LocalDateTime rangeStart = START.plusDays(1).plusMinutes(15);
        LocalDateTime rangeEnd = START.plusDays(1).plusHours(2);
        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, rangeStart, rangeEnd))
                .thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents(1L, rangeStart, rangeEnd, null, null);

        assertThat(responses).extracting(EventResponse::getStartTime).containsExactly(START.plusDays(1));
    }

    @Test
    void recurringEventStartingBeforeRangeStillProducesLaterOccurrences() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event = recurringEventWith(1L, calendar, RecurrenceType.WEEKLY);

        // The stored event started weeks before the requested range.
        LocalDateTime rangeStart = START.plusWeeks(4);
        LocalDateTime rangeEnd = START.plusWeeks(5);
        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, rangeStart, rangeEnd))
                .thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents(1L, rangeStart, rangeEnd, null, null);

        assertThat(responses).extracting(EventResponse::getStartTime).containsExactly(START.plusWeeks(4));
    }

    @Test
    void noDateRangeReturnsRecurringEventOnceUnexpanded() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event = recurringEventWith(1L, calendar, RecurrenceType.DAILY);

        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, null, null, null, null))
                .thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents(1L, null, null, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStartTime()).isEqualTo(START);
    }

    @Test
    void listEventsAppliesCombinedFiltersTogether() {
        User owner = userWith(1L);
        Calendar calendar = calendarWith(2L, owner);
        Category category = categoryWith(3L, owner);
        Event event = eventWith(1L, "Standup", calendar, category);

        when(calendarRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(calendar));
        when(categoryRepository.findByIdAndOwnerId(3L, 1L)).thenReturn(Optional.of(category));
        when(eventRepository.findByCalendarOwnerIdAndFilters(1L, 2L, 3L, null, null)).thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents(1L, null, null, 2L, 3L);

        assertThat(responses).extracting(EventResponse::getTitle).containsExactly("Standup");
        verify(eventRepository).findByCalendarOwnerIdAndFilters(1L, 2L, 3L, null, null);
    }

    @Test
    void gettingAnotherUsersOrMissingEventThrowsEventNotFoundException() {
        when(eventRepository.findByIdAndCalendarOwnerId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(5L, 2L))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void updateEventChangesSupportedFields() {
        User owner = userWith(1L);
        Calendar calendar = calendarWith(2L, owner);
        Event event = eventWith(5L, "Standup", calendar, null);

        EventRequest request = eventRequest(2L, null);
        request.setTitle("Renamed");
        request.setLocation("Room 2");

        when(eventRepository.findByIdAndCalendarOwnerId(5L, 1L)).thenReturn(Optional.of(event));
        when(calendarRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(calendar));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.updateEvent(5L, request, 1L);

        assertThat(response.getTitle()).isEqualTo("Renamed");
        assertThat(response.getLocation()).isEqualTo("Room 2");
    }

    @Test
    void updateEventWithNullCategoryClearsExistingCategory() {
        User owner = userWith(1L);
        Calendar calendar = calendarWith(2L, owner);
        Category category = categoryWith(3L, owner);
        Event event = eventWith(5L, "Standup", calendar, category);

        EventRequest request = eventRequest(2L, null);

        when(eventRepository.findByIdAndCalendarOwnerId(5L, 1L)).thenReturn(Optional.of(event));
        when(calendarRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(calendar));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.updateEvent(5L, request, 1L);

        assertThat(response.getCategoryId()).isNull();
    }

    @Test
    void deleteOwnedEventCallsRepositoryDelete() {
        Calendar calendar = calendarWith(2L, userWith(1L));
        Event event = eventWith(5L, "Standup", calendar, null);
        when(eventRepository.findByIdAndCalendarOwnerId(5L, 1L)).thenReturn(Optional.of(event));

        eventService.deleteEvent(5L, 1L);

        verify(eventRepository).delete(event);
    }

    private static User userWith(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Calendar calendarWith(Long id, User owner) {
        Calendar calendar = new Calendar();
        calendar.setId(id);
        calendar.setName("Work");
        calendar.setColor("#4A90E2");
        calendar.setOwner(owner);
        return calendar;
    }

    private static Category categoryWith(Long id, User owner) {
        Category category = new Category();
        category.setId(id);
        category.setName("Work");
        category.setColor("#4A90E2");
        category.setOwner(owner);
        return category;
    }

    private static Event eventWith(Long id, String title, Calendar calendar, Category category) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setStartTime(START);
        event.setEndTime(END);
        event.setRecurrenceType(RecurrenceType.NONE);
        event.setCalendar(calendar);
        event.setCategory(category);
        return event;
    }

    private static Event recurringEventWith(Long id, Calendar calendar, RecurrenceType recurrenceType) {
        Event event = eventWith(id, "Standup", calendar, null);
        event.setRecurrenceType(recurrenceType);
        return event;
    }

    private static EventRequest eventRequest(Long calendarId, Long categoryId) {
        EventRequest request = new EventRequest();
        request.setTitle("Standup");
        request.setStartTime(START);
        request.setEndTime(END);
        request.setCalendarId(calendarId);
        request.setCategoryId(categoryId);
        return request;
    }
}
