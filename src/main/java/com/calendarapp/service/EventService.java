package com.calendarapp.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.calendarapp.dto.EventRequest;
import com.calendarapp.dto.EventResponse;
import com.calendarapp.dto.EventTimeUpdateRequest;
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

// Handles event CRUD for the authenticated user. An Event has no owner column of
// its own - ownership is derived through its Calendar, so every read/update/delete
// looks up the event by (id, calendar owner id) together. Creating or updating an
// event also checks that the requested calendar (and category, if supplied)
// belong to the same authenticated user before the event is allowed to use them.
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CalendarRepository calendarRepository;
    private final CategoryRepository categoryRepository;

    public EventService(
            EventRepository eventRepository,
            CalendarRepository calendarRepository,
            CategoryRepository categoryRepository) {
        this.eventRepository = eventRepository;
        this.calendarRepository = calendarRepository;
        this.categoryRepository = categoryRepository;
    }

    public EventResponse createEvent(EventRequest request, User owner) {
        Calendar calendar = findOwnedCalendar(request.getCalendarId(), owner.getId());
        Category category = null;
        if (request.getCategoryId() != null) {
            category = findOwnedCategory(request.getCategoryId(), owner.getId());
        }
        validateTimes(request.getStartTime(), request.getEndTime());

        Event event = new Event();
        event.setCalendar(calendar);
        event.setCategory(category);
        applyRequest(event, request);

        Event savedEvent = eventRepository.save(event);
        return toResponse(savedEvent);
    }

    public List<EventResponse> listEvents(
            Long ownerId, LocalDateTime start, LocalDateTime end, Long calendarId, Long categoryId) {
        validateDateRangeFilter(start, end);
        if (calendarId != null) {
            findOwnedCalendar(calendarId, ownerId);
        }
        if (categoryId != null) {
            findOwnedCategory(categoryId, ownerId);
        }

        List<Event> events = eventRepository.findByCalendarOwnerIdAndFilters(ownerId, calendarId, categoryId, start, end);

        // With no date range, every stored event is returned once, unexpanded - the
        // same behavior as before recurrence expansion existed.
        if (start == null || end == null) {
            return events.stream().map(EventService::toResponse).toList();
        }

        List<EventResponse> responses = new ArrayList<>();
        for (Event event : events) {
            if (event.getRecurrenceType() == RecurrenceType.NONE) {
                responses.add(toResponse(event));
            } else {
                responses.addAll(expandOccurrences(event, start, end));
            }
        }
        return responses;
    }

    // Generates one EventResponse per occurrence of a recurring event that overlaps
    // [rangeStart, rangeEnd). Occurrences are calculated in memory only - nothing
    // here is saved back to the database. Every occurrence keeps the original
    // duration and moves forward from the stored startTime one interval at a time,
    // stopping as soon as an occurrence can no longer start before rangeEnd.
    private static List<EventResponse> expandOccurrences(Event event, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        List<EventResponse> occurrences = new ArrayList<>();
        Duration duration = Duration.between(event.getStartTime(), event.getEndTime());

        LocalDateTime occurrenceStart = event.getStartTime();
        while (occurrenceStart.isBefore(rangeEnd)) {
            LocalDateTime occurrenceEnd = occurrenceStart.plus(duration);
            if (occurrenceEnd.isAfter(rangeStart)) {
                occurrences.add(toResponse(event, occurrenceStart, occurrenceEnd));
            }

            occurrenceStart = switch (event.getRecurrenceType()) {
                case DAILY -> occurrenceStart.plusDays(1);
                case WEEKLY -> occurrenceStart.plusWeeks(1);
                case MONTHLY -> occurrenceStart.plusMonths(1);
                case NONE -> throw new IllegalStateException("NONE events are not expanded");
            };
        }
        return occurrences;
    }

    public EventResponse getEvent(Long eventId, Long ownerId) {
        Event event = findOwnedEvent(eventId, ownerId);
        return toResponse(event);
    }

    public EventResponse updateEvent(Long eventId, EventRequest request, Long ownerId) {
        Event event = findOwnedEvent(eventId, ownerId);
        Calendar calendar = findOwnedCalendar(request.getCalendarId(), ownerId);
        Category category = null;
        if (request.getCategoryId() != null) {
            category = findOwnedCategory(request.getCategoryId(), ownerId);
        }
        validateTimes(request.getStartTime(), request.getEndTime());

        event.setCalendar(calendar);
        event.setCategory(category);
        applyRequest(event, request);

        Event savedEvent = eventRepository.save(event);
        return toResponse(savedEvent);
    }

    public void deleteEvent(Long eventId, Long ownerId) {
        Event event = findOwnedEvent(eventId, ownerId);
        eventRepository.delete(event);
    }

    // For drag/resize from the calendar UI: changes only startTime/endTime, leaving
    // title, calendar, category, recurrenceType, and everything else untouched. For
    // a recurring event, this moves the whole stored series, since recurrence
    // expansion always recalculates occurrences from these same stored values.
    public EventResponse updateEventTime(Long eventId, EventTimeUpdateRequest request, Long ownerId) {
        Event event = findOwnedEvent(eventId, ownerId);
        validateTimes(request.getStartTime(), request.getEndTime());

        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());

        Event savedEvent = eventRepository.save(event);
        return toResponse(savedEvent);
    }

    // Copies the simple, non-relationship fields from the request onto the entity.
    // Calendar/category are set separately since they need ownership checks first.
    private static void applyRequest(Event event, EventRequest request) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setAllDay(request.isAllDay());
        event.setRecurrenceType(request.getRecurrenceType());
        event.setReminderOffsetMinutes(request.getReminderOffsetMinutes());
    }

    private static void validateTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidEventTimeException("Event end time must be after start time");
        }
    }

    // start/end are optional filters, but only as a pair: both missing means "no date
    // filter", one missing is ambiguous, and end must still be after start.
    private static void validateDateRangeFilter(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return;
        }
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidEventTimeException("Start and end must form a valid date range");
        }
    }

    // Shared by get/update/delete: looks up an event scoped to its calendar's owner,
    // or throws EventNotFoundException if it doesn't exist or belongs to someone else.
    private Event findOwnedEvent(Long eventId, Long ownerId) {
        return eventRepository.findByIdAndCalendarOwnerId(eventId, ownerId)
                .orElseThrow(EventNotFoundException::new);
    }

    private Calendar findOwnedCalendar(Long calendarId, Long ownerId) {
        return calendarRepository.findByIdAndOwnerId(calendarId, ownerId)
                .orElseThrow(CalendarNotFoundException::new);
    }

    private Category findOwnedCategory(Long categoryId, Long ownerId) {
        return categoryRepository.findByIdAndOwnerId(categoryId, ownerId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private static EventResponse toResponse(Event event) {
        return toResponse(event, event.getStartTime(), event.getEndTime());
    }

    // Builds a response for either the stored event itself, or one generated
    // occurrence of it - startTime/endTime are passed in separately so a recurring
    // occurrence can reuse all of the event's other fields (id, title, calendar,
    // category, etc.) while substituting its own calculated start/end.
    private static EventResponse toResponse(Event event, LocalDateTime startTime, LocalDateTime endTime) {
        Calendar calendar = event.getCalendar();
        Category category = event.getCategory();

        Long categoryId = null;
        String categoryName = null;
        String categoryColor = null;
        if (category != null) {
            categoryId = category.getId();
            categoryName = category.getName();
            categoryColor = category.getColor();
        }

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                startTime,
                endTime,
                event.isAllDay(),
                event.getRecurrenceType(),
                event.getReminderOffsetMinutes(),
                calendar.getId(),
                calendar.getName(),
                categoryId,
                categoryName,
                categoryColor);
    }
}
