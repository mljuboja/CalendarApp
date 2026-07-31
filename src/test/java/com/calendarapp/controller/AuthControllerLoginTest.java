package com.calendarapp.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.calendarapp.configuration.SecurityConfig;
import com.calendarapp.dto.LoginRequest;
import com.calendarapp.dto.LoginResponse;
import com.calendarapp.exception.InvalidCredentialsException;
import com.calendarapp.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;

// MVC test for just the POST /api/auth/login endpoint.
// Imports the real SecurityConfig instead of turning off security filters, so this
// actually runs through the real permit-all/CSRF-disabled setup. GlobalExceptionHandler
// gets picked up automatically too, so error responses are the real ones, not faked.
// AuthenticationService itself is mocked - we're only testing the web layer here.
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void validLoginReturns200WithTokenAndNoPasswordHash() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "correct-password");
        LoginResponse response = new LoginResponse(
                "header.payload.signature", "Bearer", 3_600_000L, 1L, "Jane", "Doe", "jane@example.com");

        given(authenticationService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("header.payload.signature"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMilliseconds").value(3_600_000))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void wrongCredentialsReturns401WithError() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "wrong-password");

        given(authenticationService.login(any(LoginRequest.class)))
                .willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void wrongRequestReturns400WithErrors() throws Exception {
        String blankFieldsJson = "{\"email\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankFieldsJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
