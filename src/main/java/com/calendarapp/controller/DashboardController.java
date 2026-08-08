package com.calendarapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.calendarapp.dto.DashboardResponse;
import com.calendarapp.entity.User;
import com.calendarapp.service.DashboardService;

// Dashboard summary for the authenticated user. Requires a valid JWT - see
// JwtAuthenticationFilter and SecurityConfig.
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse getDashboard(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return dashboardService.getDashboard(user.getId());
    }
}
