package com.calendarapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.calendarapp.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwnerId(Long ownerId);
}
