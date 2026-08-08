package com.calendarapp.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.calendarapp.dto.TaskRequest;
import com.calendarapp.dto.TaskResponse;
import com.calendarapp.dto.TaskStatusUpdateRequest;
import com.calendarapp.entity.User;
import com.calendarapp.service.TaskService;

// Task CRUD, scoped to whichever user the JWT identifies. Requires a valid
// JWT - see JwtAuthenticationFilter and SecurityConfig.
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        TaskResponse response = taskService.createTask(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<TaskResponse> listTasks(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return taskService.listTasks(user.getId());
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return taskService.getTask(id, user.getId());
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id, @Valid @RequestBody TaskRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return taskService.updateTask(id, request, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        taskService.deleteTask(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateTaskStatus(
            @PathVariable Long id, @Valid @RequestBody TaskStatusUpdateRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return taskService.updateTaskStatus(id, request, user.getId());
    }
}
