package com.calendarapp.controller;

import java.util.List;

import jakarta.validation.Valid;

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
import org.springframework.web.bind.annotation.RestController;

import com.calendarapp.dto.CalendarRequest;
import com.calendarapp.dto.CalendarResponse;
import com.calendarapp.entity.User;
import com.calendarapp.service.CalendarService;

// Calendar CRUD, scoped to whichever user the JWT identifies. Requires a valid
// JWT - see JwtAuthenticationFilter and SecurityConfig.
@RestController
@RequestMapping("/api/calendars")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PostMapping
    public ResponseEntity<CalendarResponse> createCalendar(
            @Valid @RequestBody CalendarRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CalendarResponse response = calendarService.createCalendar(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<CalendarResponse> listCalendars(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return calendarService.listCalendars(user.getId());
    }

    @GetMapping("/{id}")
    public CalendarResponse getCalendar(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return calendarService.getCalendar(id, user.getId());
    }

    @PutMapping("/{id}")
    public CalendarResponse updateCalendar(
            @PathVariable Long id, @Valid @RequestBody CalendarRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return calendarService.updateCalendar(id, request, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCalendar(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        calendarService.deleteCalendar(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
