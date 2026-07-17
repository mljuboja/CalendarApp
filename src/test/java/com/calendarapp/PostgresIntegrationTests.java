package com.calendarapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.calendarapp.repository.CalendarRepository;
import com.calendarapp.repository.CategoryRepository;
import com.calendarapp.repository.EventRepository;
import com.calendarapp.repository.TaskRepository;
import com.calendarapp.repository.UserRepository;

/**
 * Verifies the backend foundation end-to-end against a real PostgreSQL database:
 * Spring context starts, the datasource connects, Flyway applies
 * {@code V1__init_schema.sql}, and Hibernate validates entities against that schema.
 *
 * <p><b>Requires PostgreSQL to be running</b> (e.g. {@code docker compose up -d})
 * and a valid {@code .env} sourced into the environment. This test is tagged
 * "integration" and is excluded from the default {@code mvn test} run (see pom.xml).
 * Run it explicitly with: {@code mvn test -DexcludedGroups=}
 */
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
    void applicationContextStartsAndRepositoriesAreWired() {
        // If the context failed to start (bad DB connection, Flyway migration error,
        // or Hibernate schema-validation mismatch), this test never reaches this line.
        assertThat(userRepository).isNotNull();
        assertThat(calendarRepository).isNotNull();
        assertThat(categoryRepository).isNotNull();
        assertThat(eventRepository).isNotNull();
        assertThat(taskRepository).isNotNull();
    }

    @Test
    void repositoriesCanQueryTheValidatedSchema() {
        // Exercises a real query against every table Flyway created, confirming
        // Hibernate's schema validation matches reality (not just that beans exist).
        assertThat(userRepository.findAll()).isNotNull();
        assertThat(calendarRepository.findAll()).isNotNull();
        assertThat(categoryRepository.findAll()).isNotNull();
        assertThat(eventRepository.findAll()).isNotNull();
        assertThat(taskRepository.findAll()).isNotNull();
    }
}
