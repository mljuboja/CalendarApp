package com.calendarapp.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.calendarapp.entity.Task;
import com.calendarapp.entity.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwnerId(Long ownerId);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerIdAndStatus(Long ownerId, TaskStatus status);

    // Finds the user's incomplete tasks that are due today or later,
    // ordered from the nearest due date to the farthest.
    @Query("""
           SELECT t FROM Task t
           WHERE t.owner.id = :ownerId
           AND t.status <> :completedStatus
           AND t.dueDate >= :today
           ORDER BY t.dueDate ASC
           """)
    List<Task> findUpcomingTasks(
            @Param("ownerId") Long ownerId,
            @Param("completedStatus") TaskStatus completedStatus,
            @Param("today") LocalDate today);
}
