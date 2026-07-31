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
}
