package com.calendarapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.calendarapp.configuration.SecurityConfig;
import com.calendarapp.dto.TaskRequest;
import com.calendarapp.dto.TaskResponse;
import com.calendarapp.dto.TaskStatusUpdateRequest;
import com.calendarapp.entity.Priority;
import com.calendarapp.entity.TaskStatus;
import com.calendarapp.entity.User;
import com.calendarapp.exception.TaskNotFoundException;
import com.calendarapp.repository.UserRepository;
import com.calendarapp.security.JwtAuthenticationEntryPoint;
import com.calendarapp.security.JwtService;
import com.calendarapp.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;

// MVC test for TaskController, running through the real SecurityConfig so
// authentication is actually enforced. TaskService is mocked - only the web
// layer (status codes, validation, auth wiring) is under test here.
// JwtService/UserRepository are mocked to satisfy JwtAuthenticationFilter's
// constructor; "authenticated" requests fake a valid token the same way
// CalendarControllerTest/EventControllerTest do.
@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void authenticatedCreateReturns201() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        TaskRequest request = taskRequest();
        TaskResponse response = taskResponse();

        given(taskService.createTask(any(TaskRequest.class), any(User.class))).willReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Write report"));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        String blankFieldsJson = "{\"title\":\"\",\"priority\":null,\"status\":null}";

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankFieldsJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void missingTaskReturns404() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        given(taskService.getTask(anyLong(), anyLong())).willThrow(new TaskNotFoundException());

        mockMvc.perform(get("/api/tasks/99").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void statusPatchReturns200() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.COMPLETED);
        TaskResponse response = new TaskResponse(
                10L, "Write report", "Q3 summary", LocalDate.of(2026, 2, 1), Priority.HIGH, TaskStatus.COMPLETED);

        given(taskService.updateTaskStatus(anyLong(), any(TaskStatusUpdateRequest.class), anyLong()))
                .willReturn(response);

        mockMvc.perform(patch("/api/tasks/10/status")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteReturns204() throws Exception {
        givenAuthenticatedUser(1L, "jane@example.com");

        mockMvc.perform(delete("/api/tasks/5").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    private void givenAuthenticatedUser(Long userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        given(jwtService.extractEmail("valid-token")).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    }

    private static TaskRequest taskRequest() {
        return new TaskRequest("Write report", "Q3 summary", LocalDate.of(2026, 2, 1), Priority.HIGH, TaskStatus.TODO);
    }

    private static TaskResponse taskResponse() {
        return new TaskResponse(
                10L, "Write report", "Q3 summary", LocalDate.of(2026, 2, 1), Priority.HIGH, TaskStatus.TODO);
    }
}
