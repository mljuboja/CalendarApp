package com.calendarapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.calendarapp.dto.CalendarRequest;
import com.calendarapp.dto.CalendarResponse;
import com.calendarapp.entity.Calendar;
import com.calendarapp.entity.User;
import com.calendarapp.exception.CalendarNotFoundException;
import com.calendarapp.repository.CalendarRepository;

// Handles calendar CRUD for the authenticated user. Every read/update/delete looks
// up the calendar by (id, ownerId) together, so a user can never touch a calendar
// that isn't theirs.
@Service
public class CalendarService {

    private final CalendarRepository calendarRepository;

    public CalendarService(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    // Owner is taken from the authenticated principal, never from the request.
    public CalendarResponse createCalendar(CalendarRequest request, User owner) {
        Calendar calendar = new Calendar();
        calendar.setName(request.getName());
        calendar.setColor(request.getColor());
        calendar.setOwner(owner);

        Calendar savedCalendar = calendarRepository.save(calendar);
        return toResponse(savedCalendar);
    }

    public List<CalendarResponse> listCalendars(Long ownerId) {
        return calendarRepository.findByOwnerId(ownerId).stream()
                .map(CalendarService::toResponse)
                .toList();
    }

    public CalendarResponse getCalendar(Long calendarId, Long ownerId) {
        Calendar calendar = findOwnedCalendar(calendarId, ownerId);
        return toResponse(calendar);
    }

    public CalendarResponse updateCalendar(Long calendarId, CalendarRequest request, Long ownerId) {
        Calendar calendar = findOwnedCalendar(calendarId, ownerId);
        calendar.setName(request.getName());
        calendar.setColor(request.getColor());

        Calendar savedCalendar = calendarRepository.save(calendar);
        return toResponse(savedCalendar);
    }

    public void deleteCalendar(Long calendarId, Long ownerId) {
        Calendar calendar = findOwnedCalendar(calendarId, ownerId);
        calendarRepository.delete(calendar);
    }

    // Shared by get/update/delete: looks up a calendar scoped to its owner, or
    // throws CalendarNotFoundException if it doesn't exist or belongs to someone else.
    private Calendar findOwnedCalendar(Long calendarId, Long ownerId) {
        return calendarRepository.findByIdAndOwnerId(calendarId, ownerId)
                .orElseThrow(CalendarNotFoundException::new);
    }

    private static CalendarResponse toResponse(Calendar calendar) {
        return new CalendarResponse(calendar.getId(), calendar.getName(), calendar.getColor());
    }
}
