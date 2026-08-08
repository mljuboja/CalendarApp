package com.calendarapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.calendarapp.dto.TaskRequest;
import com.calendarapp.dto.TaskResponse;
import com.calendarapp.dto.TaskStatusUpdateRequest;
import com.calendarapp.entity.Task;
import com.calendarapp.entity.User;
import com.calendarapp.exception.TaskNotFoundException;
import com.calendarapp.repository.TaskRepository;

// Handles task CRUD for the authenticated user. A Task belongs directly to its
// owner (unlike Event, which is owned through Calendar), so every
// read/update/delete looks the task up by (id, ownerId) together.
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // Owner is taken from the authenticated principal, never from the request.
    public TaskResponse createTask(TaskRequest request, User owner) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setOwner(owner);

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    public List<TaskResponse> listTasks(Long ownerId) {
        return taskRepository.findByOwnerId(ownerId).stream()
                .map(TaskService::toResponse)
                .toList();
    }

    public TaskResponse getTask(Long taskId, Long ownerId) {
        Task task = findOwnedTask(taskId, ownerId);
        return toResponse(task);
    }

    public TaskResponse updateTask(Long taskId, TaskRequest request, Long ownerId) {
        Task task = findOwnedTask(taskId, ownerId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    public void deleteTask(Long taskId, Long ownerId) {
        Task task = findOwnedTask(taskId, ownerId);
        taskRepository.delete(task);
    }

    // For a quick "mark as done" action from the UI: changes only status,
    // leaving title, description, dueDate, and priority untouched. Any of the
    // three TaskStatus values may be set directly - no transition rules.
    public TaskResponse updateTaskStatus(Long taskId, TaskStatusUpdateRequest request, Long ownerId) {
        Task task = findOwnedTask(taskId, ownerId);
        task.setStatus(request.getStatus());

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    // Shared by get/update/delete/updateStatus: looks up a task scoped to its
    // owner, or throws TaskNotFoundException if it doesn't exist or belongs to
    // someone else.
    private Task findOwnedTask(Long taskId, Long ownerId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(TaskNotFoundException::new);
    }

    private static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority(),
                task.getStatus());
    }
}
