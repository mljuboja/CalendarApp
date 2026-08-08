package com.calendarapp.controller;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.calendarapp.dto.EventRequest;
import com.calendarapp.dto.EventResponse;
import com.calendarapp.entity.User;
import com.calendarapp.service.EventService;

// Event CRUD, scoped to whichever user the JWT identifies. Requires a valid
// JWT - see JwtAuthenticationFilter and SecurityConfig.
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        EventResponse response = eventService.createEvent(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // start/end/calendarId/categoryId are all optional and combine with AND
    // behavior; omitting all of them returns every event the caller owns, same
    // as before filtering existed.
    @GetMapping
    public List<EventResponse> listEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) Long calendarId,
            @RequestParam(required = false) Long categoryId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return eventService.listEvents(user.getId(), start, end, calendarId, categoryId);
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return eventService.getEvent(id, user.getId());
    }

    @PutMapping("/{id}")
    public EventResponse updateEvent(
            @PathVariable Long id, @Valid @RequestBody EventRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return eventService.updateEvent(id, request, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.deleteEvent(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
