package com.calendarapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.calendarapp.entity.Calendar;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    List<Calendar> findByOwnerId(Long ownerId);
}
