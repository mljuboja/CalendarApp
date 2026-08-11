package com.calendarapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.calendarapp.repository.CalendarRepository;
import com.calendarapp.repository.CategoryRepository;
import com.calendarapp.repository.EventRepository;
import com.calendarapp.repository.TaskRepository;
import com.calendarapp.repository.UserRepository;

// This test makes sure the app can actually connect to a real Postgres database,
// the tables get created, and everything is set up right.
//
// You need Postgres running for this to work (docker compose up -d) and a .env file.
// This is an "integration" test so it does not run with the normal mvn test command.
// To run it: mvn test -DexcludedGroups=
@Tag("integration")
@SpringBootTest
class PostgresIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void appStartsAndRepositoriesWork() {
        // If something went wrong on startup (like the database not connecting),
        // the test will fail before it even gets here.
        assertThat(userRepository).isNotNull();
        assertThat(calendarRepository).isNotNull();
        assertThat(categoryRepository).isNotNull();
        assertThat(eventRepository).isNotNull();
        assertThat(taskRepository).isNotNull();
    }

    @Test
    void repositoriesCanGetData() {
        // Just checking we can actually pull data back from each table.
        assertThat(userRepository.findAll()).isNotNull();
        assertThat(calendarRepository.findAll()).isNotNull();
        assertThat(categoryRepository.findAll()).isNotNull();
        assertThat(eventRepository.findAll()).isNotNull();
        assertThat(taskRepository.findAll()).isNotNull();
    }

    // Regression test for a real bug: PostgreSQL couldn't determine the data type
    // of the "start"/"end" date-range parameters in
    // EventRepository.findByCalendarOwnerIdAndFilters when they were compared with
    // "IS NULL" instead of a typed column, and threw
    // "could not determine data type of parameter $N" (SQLState 42P18). This can
    // only be caught by actually running the query against a real Postgres
    // database - a Mockito-based unit test never sends real SQL, so it can't
    // reproduce this. No test data is needed; this just proves the query itself is
    // valid SQL that Postgres can run with a real date range.
    @Test
    void dateRangeFilterQueryRunsAgainstRealPostgres() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);

        assertThat(eventRepository.findByCalendarOwnerIdAndFilters(-1L, null, null, start, end))
                .isNotNull();
    }
}
