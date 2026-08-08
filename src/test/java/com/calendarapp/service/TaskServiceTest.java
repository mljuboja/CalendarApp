package com.calendarapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.calendarapp.dto.TaskRequest;
import com.calendarapp.dto.TaskResponse;
import com.calendarapp.dto.TaskStatusUpdateRequest;
import com.calendarapp.entity.Priority;
import com.calendarapp.entity.Task;
import com.calendarapp.entity.TaskStatus;
import com.calendarapp.entity.User;
import com.calendarapp.exception.TaskNotFoundException;
import com.calendarapp.repository.TaskRepository;

// Tests for TaskService. TaskRepository is mocked so these don't need a real
// database - ownership is enforced by scoping every lookup by owner ID.
class TaskServiceTest {

    private TaskRepository taskRepository;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        taskService = new TaskService(taskRepository);
    }

    @Test
    void createTaskAssignsAuthenticatedUserAsOwner() {
        User owner = new User();
        owner.setId(1L);
        TaskRequest request = new TaskRequest("Write report", "Q3 summary", LocalDate.of(2026, 2, 1),
                Priority.HIGH, TaskStatus.TODO);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(10L);
            return task;
        });

        TaskResponse response = taskService.createTask(request, owner);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Write report");
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getOwner()).isEqualTo(owner);
    }

    @Test
    void listTasksUsesAuthenticatedOwnerId() {
        Task task1 = taskWith(1L, "Write report", Priority.HIGH, TaskStatus.TODO);
        Task task2 = taskWith(2L, "Review PR", Priority.LOW, TaskStatus.IN_PROGRESS);

        when(taskRepository.findByOwnerId(1L)).thenReturn(List.of(task1, task2));

        List<TaskResponse> responses = taskService.listTasks(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TaskResponse::getTitle).containsExactly("Write report", "Review PR");
        verify(taskRepository).findByOwnerId(1L);
    }

    @Test
    void getOwnedTaskSucceeds() {
        Task task = taskWith(5L, "Write report", Priority.HIGH, TaskStatus.TODO);
        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTask(5L, 1L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getTitle()).isEqualTo("Write report");
    }

    @Test
    void getMissingOrNotOwnedTaskThrowsTaskNotFoundException() {
        when(taskRepository.findByIdAndOwnerId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(5L, 2L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateTaskChangesEditableFields() {
        Task task = taskWith(5L, "Write report", Priority.HIGH, TaskStatus.TODO);
        TaskRequest request = new TaskRequest("Renamed", "Updated description", LocalDate.of(2026, 3, 1),
                Priority.LOW, TaskStatus.IN_PROGRESS);

        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTask(5L, request, 1L);

        assertThat(response.getTitle()).isEqualTo("Renamed");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(response.getPriority()).isEqualTo(Priority.LOW);
        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void deleteOwnedTaskCallsRepositoryDelete() {
        Task task = taskWith(5L, "Write report", Priority.HIGH, TaskStatus.TODO);
        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(task));

        taskService.deleteTask(5L, 1L);

        verify(taskRepository).delete(task);
    }

    @Test
    void updateTaskStatusChangesOnlyStatus() {
        Task task = taskWith(5L, "Write report", Priority.HIGH, TaskStatus.TODO);
        task.setDescription("Q3 summary");
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.COMPLETED);

        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskStatus(5L, request, 1L);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.getTitle()).isEqualTo("Write report");
        assertThat(response.getDescription()).isEqualTo("Q3 summary");
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
    }

    private static Task taskWith(Long id, String title, Priority priority, TaskStatus status) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setPriority(priority);
        task.setStatus(status);
        return task;
    }
}
