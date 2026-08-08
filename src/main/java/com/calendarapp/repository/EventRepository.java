package com.calendarapp.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.calendarapp.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCalendarId(Long calendarId);

    Optional<Event> findByIdAndCalendarOwnerId(Long id, Long ownerId);

    // Lists an owner's events, optionally narrowed by calendar, category, and/or a
    // date range. Each (:param IS NULL OR ...) clause simply skips that filter when
    // the caller didn't supply it, so this one query covers every combination
    // (including no filters at all) without a separate method per combination.
    // LEFT JOIN on category keeps uncategorized events in the results unless the
    // caller filters by category - an inner join would drop them even with no
    // category filter applied.
    @Query("SELECT e FROM Event e LEFT JOIN e.category c "
            + "WHERE e.calendar.owner.id = :ownerId "
            + "AND (:calendarId IS NULL OR e.calendar.id = :calendarId) "
            + "AND (:categoryId IS NULL OR c.id = :categoryId) "
            + "AND (:start IS NULL OR e.startTime < :end) "
            + "AND (:end IS NULL OR e.endTime > :start)")
    List<Event> findByCalendarOwnerIdAndFilters(
            @Param("ownerId") Long ownerId,
            @Param("calendarId") Long calendarId,
            @Param("categoryId") Long categoryId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
