package com.calendarapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.calendarapp.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCalendarId(Long calendarId);
}
