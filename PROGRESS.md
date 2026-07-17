# Project Progress

## Phase 1 — Domain Model (Complete)

- Defined the core domain model: `User`, `Calendar`, `Category`, `Event`, `Task`.
- Defined supporting enums: `RecurrenceType`, `Priority`, `TaskStatus`.

## Phase 2 — Backend Foundation (Complete)

- Bootstrapped the Spring Boot project (Java 21, Spring Boot 3.3.4, Maven).
- Added dependencies: Spring Web, Spring Data JPA, Spring Validation, spring-security-crypto (for `BCryptPasswordEncoder` only — no auth filters/config yet), PostgreSQL driver, Flyway (+ `flyway-database-postgresql`), springdoc-openapi, Lombok.
- Added JPA entities for all five domain objects, matching the Phase 1 model.
- Added Spring Data JPA repositories: `UserRepository`, `CalendarRepository`, `CategoryRepository`, `EventRepository`, `TaskRepository`.
- Added Flyway migration `V1__init_schema.sql` creating `users`, `calendars`, `categories`, `events`, `tasks` tables with constraints and indexes.
- Configured Hibernate to `validate` (never generate/alter) the schema — Flyway is the single source of truth for the database structure.
- Added `docker-compose.yml` to run PostgreSQL 16 locally, configured via `.env` (see `.env.example`).
- Added `PostgresIntegrationTests` (tagged `integration`, excluded from default `mvn test` runs) to verify end-to-end: context startup, DB connection, Flyway migration, Hibernate schema validation, and repository queries against a real PostgreSQL instance.
- Removed legacy/unused project files (`src/Main.java`, IDE-specific `.idea/` files and `CalendarApp.iml`) that predated the Spring Boot setup.

### Phase 2 Verification (Confirmed)

Ran locally against Docker Desktop + PostgreSQL 16:

```bash
docker compose up -d
docker compose ps
mvn test -DexcludedGroups=
```

Results:
- PostgreSQL container started successfully and reported healthy.
- Flyway applied `V1__init_schema.sql` cleanly to an empty schema.
- Hibernate schema validation passed (entities match the Flyway-created schema).
- All five repositories (`UserRepository`, `CalendarRepository`, `CategoryRepository`, `EventRepository`, `TaskRepository`) loaded as Spring beans.
- Both integration tests in `PostgresIntegrationTests` passed (`applicationContextStartsAndRepositoriesAreWired`, `repositoriesCanQueryTheValidatedSchema`).

## Phase 3 — Not Started

Not yet planned/scoped in this document.
