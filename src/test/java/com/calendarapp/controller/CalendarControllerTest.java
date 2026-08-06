package com.calendarapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.calendarapp.configuration.SecurityConfig;
import com.calendarapp.dto.CalendarRequest;
import com.calendarapp.dto.CalendarResponse;
import com.calendarapp.entity.User;
import com.calendarapp.exception.CalendarNotFoundException;
import com.calendarapp.repository.UserRepository;
import com.calendarapp.security.JwtAuthenticationEntryPoint;
import com.calendarapp.security.JwtService;
import com.calendarapp.service.CalendarService;
import com.fasterxml.jackson.databind.ObjectMapper;

// MVC test for CalendarController, running through the real SecurityConfig so
// authentication is actually enforced. CalendarService is mocked - only the web
// layer (status codes, validation, auth wiring) is under test here.
// JwtService/UserRepository are mocked to satisfy JwtAuthenticationFilter's
// constructor; "authenticated" requests fake a valid token the same way
// UserControllerTest does.
@WebMvcTest(CalendarController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CalendarService calendarService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void authenticatedCreateReturns201() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        CalendarRequest request = new CalendarRequest("Work", "#4A90E2");
        CalendarResponse response = new CalendarResponse(10L, "Work", "#4A90E2");

        given(calendarService.createCalendar(any(CalendarRequest.class), any(User.class))).willReturn(response);

        mockMvc.perform(post("/api/calendars")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Work"))
                .andExpect(jsonPath("$.color").value("#4A90E2"));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/calendars"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        String blankFieldsJson = "{\"name\":\"\",\"color\":\"not-a-color\"}";

        mockMvc.perform(post("/api/calendars")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankFieldsJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void missingCalendarReturns404() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        given(calendarService.getCalendar(anyLong(), anyLong())).willThrow(new CalendarNotFoundException());

        mockMvc.perform(get("/api/calendars/99").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Calendar not found"));
    }

    @Test
    void deleteReturns204() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");

        mockMvc.perform(delete("/api/calendars/5").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    private void givenAuthenticatedUser(Long userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        given(jwtService.extractEmail("valid-token")).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    }
}
