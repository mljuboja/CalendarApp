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
    //
    // The date-range condition has two cases: a NONE event still needs its stored
    // startTime/endTime to overlap the range like before. A recurring event's
    // occurrences only ever move forward in time from its stored startTime, so it's
    // a candidate whenever that stored startTime is before the requested end - it
    // doesn't matter how long ago it started. EventService then works out exactly
    // which generated occurrences actually fall in the range.
    //
    // start/end are CAST to timestamp explicitly because PostgreSQL cannot always
    // figure out a bind parameter's data type on its own when that parameter is
    // compared with "IS NULL" instead of a typed column (e.g. "(:start IS NULL OR
    // ...)") - without the cast, Postgres throws "could not determine data type of
    // parameter $N" for that parameter. Casting removes the ambiguity. calendarId/
    // categoryId don't need this because Postgres can already infer their type from
    // the "= :calendarId"/"= :categoryId" comparisons elsewhere in the same clause.
    @Query("SELECT e FROM Event e LEFT JOIN e.category c "
            + "WHERE e.calendar.owner.id = :ownerId "
            + "AND (:calendarId IS NULL OR e.calendar.id = :calendarId) "
            + "AND (:categoryId IS NULL OR c.id = :categoryId) "
            + "AND (CAST(:start AS timestamp) IS NULL OR ("
            + "(e.recurrenceType = com.calendarapp.entity.RecurrenceType.NONE "
            + "AND e.startTime < CAST(:end AS timestamp) AND e.endTime > CAST(:start AS timestamp)) "
            + "OR (e.recurrenceType <> com.calendarapp.entity.RecurrenceType.NONE "
            + "AND e.startTime < CAST(:end AS timestamp))"
            + "))")
    List<Event> findByCalendarOwnerIdAndFilters(
            @Param("ownerId") Long ownerId,
            @Param("calendarId") Long calendarId,
            @Param("categoryId") Long categoryId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
