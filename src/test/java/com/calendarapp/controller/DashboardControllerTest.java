package com.calendarapp.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.calendarapp.configuration.SecurityConfig;
import com.calendarapp.dto.DashboardResponse;
import com.calendarapp.entity.User;
import com.calendarapp.repository.UserRepository;
import com.calendarapp.security.JwtAuthenticationEntryPoint;
import com.calendarapp.security.JwtService;
import com.calendarapp.service.DashboardService;

// MVC test for DashboardController, running through the real SecurityConfig so
// authentication is actually enforced. DashboardService is mocked - only the
// web layer (status codes, auth wiring, response shape) is under test here.
@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void authenticatedGetReturns200WithDashboardSections() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        DashboardResponse response = new DashboardResponse(List.of(), List.of(), 4L, 2.5);

        given(dashboardService.getDashboard(anyLong())).willReturn(response);

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todaysEvents").isArray())
                .andExpect(jsonPath("$.upcomingTasks").isArray())
                .andExpect(jsonPath("$.completedTaskCount").value(4))
                .andExpect(jsonPath("$.scheduledHoursToday").value(2.5));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private void givenAuthenticatedUser(Long userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        given(jwtService.extractEmail("valid-token")).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    }
}
