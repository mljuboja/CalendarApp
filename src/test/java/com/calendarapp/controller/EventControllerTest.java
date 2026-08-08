package com.calendarapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.calendarapp.configuration.SecurityConfig;
import com.calendarapp.dto.EventRequest;
import com.calendarapp.dto.EventResponse;
import com.calendarapp.entity.RecurrenceType;
import com.calendarapp.entity.User;
import com.calendarapp.exception.EventNotFoundException;
import com.calendarapp.exception.InvalidEventTimeException;
import com.calendarapp.repository.UserRepository;
import com.calendarapp.security.JwtAuthenticationEntryPoint;
import com.calendarapp.security.JwtService;
import com.calendarapp.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;

// MVC test for EventController, running through the real SecurityConfig so
// authentication is actually enforced. EventService is mocked - only the web
// layer (status codes, validation, auth wiring) is under test here.
// JwtService/UserRepository are mocked to satisfy JwtAuthenticationFilter's
// constructor; "authenticated" requests fake a valid token the same way
// CalendarControllerTest/CategoryControllerTest do.
@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void authenticatedCreateReturns201() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        EventRequest request = eventRequest();
        EventResponse response = eventResponse();

        given(eventService.createEvent(any(EventRequest.class), any(User.class))).willReturn(response);

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Standup"))
                .andExpect(jsonPath("$.calendarId").value(2));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        String blankFieldsJson = "{\"title\":\"\",\"calendarId\":null}";

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankFieldsJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void missingEventReturns404() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        given(eventService.getEvent(anyLong(), anyLong())).willThrow(new EventNotFoundException());

        mockMvc.perform(get("/api/events/99").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event not found"));
    }

    @Test
    void listPassesQueryParametersToService() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        given(eventService.listEvents(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L), eq(3L)))
                .willReturn(List.of(eventResponse()));

        mockMvc.perform(get("/api/events")
                        .param("start", "2026-01-01T09:00:00")
                        .param("end", "2026-01-01T10:00:00")
                        .param("calendarId", "2")
                        .param("categoryId", "3")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));

        verify(eventService).listEvents(1L, LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 10, 0), 2L, 3L);
    }

    @Test
    void listWithOnlyOneDateParameterReturns400() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        given(eventService.listEvents(eq(1L), any(LocalDateTime.class), isNull(), isNull(), isNull()))
                .willThrow(new InvalidEventTimeException("Start and end must form a valid date range"));

        mockMvc.perform(get("/api/events")
                        .param("start", "2026-01-01T09:00:00")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deleteReturns204() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");

        mockMvc.perform(delete("/api/events/5").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    private void givenAuthenticatedUser(Long userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        given(jwtService.extractEmail("valid-token")).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    }

    private static EventRequest eventRequest() {
        EventRequest request = new EventRequest();
        request.setTitle("Standup");
        request.setStartTime(LocalDateTime.of(2026, 1, 1, 9, 0));
        request.setEndTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        request.setCalendarId(2L);
        return request;
    }

    private static EventResponse eventResponse() {
        return new EventResponse(
                10L,
                "Standup",
                null,
                null,
                LocalDateTime.of(2026, 1, 1, 9, 0),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false,
                RecurrenceType.NONE,
                null,
                2L,
                "Work",
                null,
                null,
                null);
    }
}
