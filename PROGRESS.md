# Project Progress

This document is the authoritative status document for **Daymark**. It reflects
the current state of the project as of the completion of Phase 5C.

---

# Project

**Daymark** is a desktop-only, full-stack calendar application. It is intended
as a realistic software engineering portfolio project for an entry-level Java
developer — scoped and built the way a junior engineer would reasonably build
and document a project like this, not an enterprise-scale system.

## Technology Stack

### Backend

- Java 21
- Spring Boot
- PostgreSQL
- Spring Data JPA
- Hibernate
- Flyway
- Spring Security
- BCrypt
- JJWT
- Bean Validation
- Swagger / OpenAPI

### Frontend (planned)

- React
- TypeScript
- React Router
- Axios
- FullCalendar

### Development

- Maven
- Docker
- Docker Compose
- Git
- GitHub

---

# Completed Phases

## Phase 1 — Planning

Phase 1 was a design-only phase (see `PLAN.md`) that produced:

- The domain model
- Entity relationships
- REST API design
- Overall architecture
- Package structure (`controller`, `service`, `repository`, `entity`, `dto`,
  `mapper`, `config`, `exception`)
- Core business rules
- Project scope

No code was written in this phase.

---

## Phase 2 — Backend Foundation

Phase 2 completed:

- PostgreSQL configuration
- Docker Compose setup for local PostgreSQL
- Flyway migration (`V1__init_schema.sql`) as the single source of truth for
  the database schema
- Hibernate configured to validate (not generate/alter) the schema
- Environment variable configuration (`.env` / `.env.example`)
- JPA entities
- Enums
- Spring Data JPA repositories
- Integration tests
- Successful verification against a real PostgreSQL instance (Flyway
  migration + Hibernate schema validation + repository wiring)

**Entities:**

- `User`
- `Calendar`
- `Category`
- `Event`
- `Task`

**Enums:**

- `Priority`
- `TaskStatus`
- `RecurrenceType`

---

## Phase 3A — Security Foundation

Phase 3A completed:

- Spring Security dependency
- A stateless `SecurityFilterChain` (`SecurityConfig`)
- `BCryptPasswordEncoder` bean for password hashing
- `JwtProperties` (JWT configuration)
- `JwtService` (token generation and parsing)
- JWT configuration driven by environment variables
- JWT unit tests
- `JwtProperties` validation tests

At the end of Phase 3A, all endpoints still remained permit-all — no request
authentication existed yet.

---

## Phase 3B — Registration

Phase 3B completed:

- `RegistrationRequest` DTO
- `RegistrationResponse` DTO
- `AuthenticationService.register()`
- `POST /api/auth/register`
- `DuplicateEmailException`
- `ErrorResponse`
- `GlobalExceptionHandler`
- Bean Validation on registration input
- Email normalization (trim + lowercase) before persistence
- Duplicate email detection
- BCrypt password hashing
- Registration unit tests

---

## Phase 3C — Login

Phase 3C completed:

- `LoginRequest` DTO
- `LoginResponse` DTO
- `AuthenticationService.login()`
- `POST /api/auth/login`
- `InvalidCredentialsException`
- JWT generation after successful authentication
- A single, generic login failure response for both an unknown email and an
  incorrect password (avoids revealing which one was wrong)
- Login unit tests
- Login controller tests

**Explicitly not yet done as of the end of Phase 3C:**

- No JWT authentication filter exists yet
- No protected endpoints exist yet
- No `/api/users/me` endpoint exists yet
- `SecurityFilterChain` still permits all requests (`anyRequest().permitAll()`)

---

## Phase 3D — JWT Request Authentication

Phase 3D completed:

- `JwtAuthenticationFilter` — a `OncePerRequestFilter` that reads the
  `Authorization` header, validates the JWT with the existing `JwtService`,
  loads the user from `UserRepository` by the JWT's email subject claim, and
  rejects authentication if no user with that email exists
- `JwtAuthenticationEntryPoint` — returns a consistent `401 Unauthorized`
  `ErrorResponse` for any unauthenticated request to a protected endpoint
  (missing header, malformed header, expired/tampered/wrong-signature token,
  deleted user)
- `SecurityFilterChain` updated: only `POST /api/auth/register`,
  `POST /api/auth/login`, and the Swagger/OpenAPI paths remain public;
  everything else requires authentication
- `JwtAuthenticationFilter` registered before
  `UsernamePasswordAuthenticationFilter`; sessions remain stateless, CSRF/form
  login/HTTP Basic remain disabled
- The authenticated `Authentication` uses the loaded `User` entity as its
  principal, `null` credentials, and no authorities (no roles/permissions in
  this phase)
- `UserResponse` DTO
- `GET /api/users/me` (`UserController`) — reads the `User` from the
  `Authentication` parameter and returns `userId`, `firstName`, `lastName`,
  `email` (never `passwordHash` or the JWT)
- JWT filter unit tests (`JwtAuthenticationFilterTest`) covering: no header,
  malformed `Bearer` header, valid token, expired token, tampered token,
  deleted user
- `UserController` MVC tests (`UserControllerTest`) covering: `401` without a
  token, and a successful `/api/users/me` response with no `passwordHash` in
  the JSON
- Registration and login tests updated only as needed to keep working with
  the real `SecurityConfig` (added `UserRepository`/`JwtService` mocks so the
  `@WebMvcTest` slice can construct the new filter/entry point); no behavior
  changes to registration or login themselves

At the end of Phase 3D, JWT-based request authentication is fully wired in:
every endpoint except registration, login, and Swagger requires a valid
Bearer token, and a deleted user or a tampered/expired token is rejected
with `401` rather than a `500`.

Explicitly not done as part of Phase 3D (by design): roles, permissions,
admin users, refresh tokens, logout/token revocation, OAuth, Google login,
password reset, or email verification.

---

## Phase 4A — Calendar CRUD

Phase 4A completed:

- `CalendarRequest` DTO (`name`, `color`) — `name` required with a max length
  of 100 characters, `color` required and validated against the same 6-digit
  hex pattern (`^#[0-9A-Fa-f]{6}$`) already enforced by the `Calendar` entity
  and the Flyway `CHECK` constraint
- `CalendarResponse` DTO (`id`, `name`, `color` only — no owner/user info)
- `CalendarService` — create, list, get, update, delete, all scoped to the
  authenticated user
- `CalendarRepository.findByIdAndOwnerId(id, ownerId)` added alongside the
  existing `findByOwnerId(ownerId)`
- `CalendarController` (`/api/calendars`) — thin controller reading the
  authenticated `User` from the `Authentication` parameter; no ownership or
  database logic in the controller itself
  - `POST /api/calendars` → `201 Created`
  - `GET /api/calendars` → `200 OK` (only the caller's calendars)
  - `GET /api/calendars/{id}` → `200 OK`
  - `PUT /api/calendars/{id}` → `200 OK`
  - `DELETE /api/calendars/{id}` → `204 No Content`
- `CalendarNotFoundException` — generic "Calendar not found" message, used
  both when a calendar doesn't exist and when it belongs to another user, so
  the API never reveals that another user's calendar exists
- `GlobalExceptionHandler` updated to turn `CalendarNotFoundException` into a
  `404` `ErrorResponse`
- Ownership is enforced entirely through repository queries scoped by owner
  ID (`findByOwnerId`, `findByIdAndOwnerId`) — never by loading all calendars
  and filtering in Java, and never by trusting an owner ID from the client
- `CalendarServiceTest` (Mockito, no database) covering: create assigns the
  authenticated user as owner, list returns only that user's calendars, get
  succeeds for an owned calendar, get throws `CalendarNotFoundException` for
  another user's calendar, update changes name and color, delete calls the
  repository delete operation
- `CalendarControllerTest` (`@WebMvcTest`, real `SecurityConfig`) covering:
  authenticated create returns `201`, unauthenticated request returns `401`,
  invalid request returns `400`, a missing/not-owned calendar returns `404`,
  delete returns `204`

**Known limitation carried into Phase 5:** the `events` table has
`calendar_id BIGINT NOT NULL REFERENCES calendars (id)` with no
`ON DELETE CASCADE`. At the end of Phase 4A, Event CRUD did not exist yet, so
no event could reference a calendar and this was unreachable. Event CRUD now
exists as of Phase 5A, so a calendar that still has events will fail to
delete with a foreign-key violation until a policy (blocking the delete,
cascading, or reassigning events) is decided — no such policy has been
implemented yet; this remains intentionally deferred.

---

## Phase 4B — Category CRUD

Phase 4B completed:

- `CategoryRequest` DTO (`name`, `color`) — `name` required with a max length
  of 100 characters, `color` required and validated against the same 6-digit
  hex pattern (`^#[0-9A-Fa-f]{6}$`) already enforced by the `Category` entity
  and the Flyway `CHECK` constraint. This mirrors `CalendarRequest` exactly —
  no additional validation rules were introduced for Category.
- `CategoryResponse` DTO (`id`, `name`, `color` only — no owner/user info)
- `CategoryService` — create, list, get, update, delete, all scoped to the
  authenticated user, following the same structure as `CalendarService`
- `CategoryRepository.findByIdAndOwnerId(id, ownerId)` added alongside the
  existing `findByOwnerId(ownerId)`
- `CategoryController` (`/api/categories`) — thin controller reading the
  authenticated `User` from the `Authentication` parameter; no ownership or
  database logic in the controller itself
  - `POST /api/categories` → `201 Created`
  - `GET /api/categories` → `200 OK` (only the caller's categories)
  - `GET /api/categories/{id}` → `200 OK`
  - `PUT /api/categories/{id}` → `200 OK`
  - `DELETE /api/categories/{id}` → `204 No Content`
- `CategoryNotFoundException` — generic "Category not found" message, used
  both when a category doesn't exist and when it belongs to another user, so
  the API never reveals that another user's category exists
- `GlobalExceptionHandler` updated to turn `CategoryNotFoundException` into a
  `404` `ErrorResponse`
- Ownership is enforced entirely through repository queries scoped by owner
  ID (`findByOwnerId`, `findByIdAndOwnerId`) — never by loading all categories
  and filtering in Java, and never by trusting an owner ID from the client
- `CategoryServiceTest` (Mockito, no database) covering: create assigns the
  authenticated user as owner, list returns only that user's categories, get
  succeeds for an owned category, get throws `CategoryNotFoundException` for
  another user's category, update changes name and color, delete calls the
  repository delete operation
- `CategoryControllerTest` (`@WebMvcTest`, real `SecurityConfig`) covering:
  authenticated create returns `201`, unauthenticated request returns `401`,
  invalid request returns `400`, a missing/not-owned category returns `404`,
  delete returns `204`

**Existing foreign-key behavior between Event and Category (unchanged in this
phase):** `events.category_id` is `BIGINT REFERENCES categories (id)` with no
`NOT NULL` constraint — an event's category is nullable — and there is no
`ON DELETE SET NULL` or cascade behavior defined on that foreign key. At the
time of Phase 4B, Event CRUD had not been implemented yet, so the normal
application had no way to create an `Event` row that referenced a category,
meaning this constraint could not yet be triggered. Event CRUD now exists as
of Phase 5A. What should happen when deleting a category that is still
referenced by an event (block the delete, set the event's category to null,
etc.) has not been decided and remains deferred.

---

## Phase 5A — Basic Event CRUD

Phase 5A completed:

- `EventRequest` DTO (`title`, `description`, `location`, `startTime`,
  `endTime`, `allDay`, `recurrenceType`, `reminderOffsetMinutes`,
  `calendarId`, `categoryId`) — `title` required, `startTime`/`endTime`/
  `calendarId` required via `@NotNull`/`@NotBlank`; `categoryId` optional.
  Calendar/Category ownership and the `endTime > startTime` rule are checked
  in `EventService`, not with Bean Validation, since they require the
  authenticated user and the database.
- `EventResponse` DTO — the event's own fields plus `calendarId`,
  `calendarName`, `categoryId`, `categoryName`, `categoryColor`. No
  `Calendar`/`Category`/`User` entities are ever returned; the category
  fields are simply `null` when the event has no category.
- `EventService` — create, list, get, update, delete, all scoped to the
  authenticated user, following the same structure as `CalendarService`/
  `CategoryService`:
  - **Ownership through Calendar:** `Event` has no owner column of its own.
    Every read/update/delete looks the event up by (event ID, calendar owner
    ID) together, via `EventRepository.findByIdAndCalendarOwnerId`, so a user
    can never touch an event whose calendar isn't theirs.
  - **Create/update:** the requested `calendarId` is looked up with the
    existing `CalendarRepository.findByIdAndOwnerId` (reused directly, not
    through `CalendarService`) and must belong to the authenticated user, or
    `CalendarNotFoundException` is thrown. If `categoryId` is supplied, the
    same check runs against `CategoryRepository.findByIdAndOwnerId`, throwing
    `CategoryNotFoundException` if it isn't owned by the caller. A `null`
    `categoryId` clears the event's category.
  - **Time validation:** after applying the requested calendar/category/times,
    the service checks `endTime.isAfter(startTime)`; if not, it throws the
    new `InvalidEventTimeException` ("Event end time must be after start
    time").
- `EventRepository.findByCalendarOwnerId(ownerId)` and
  `findByIdAndCalendarOwnerId(id, ownerId)` added, derived by Spring Data
  directly from the `Event → Calendar → User owner` relationship (no
  in-Java filtering). The existing unused `findByCalendarId` was left as-is.
- `EventController` (`/api/events`) — thin controller reading the
  authenticated `User` from the `Authentication` parameter; no ownership or
  database logic in the controller itself
  - `POST /api/events` → `201 Created`
  - `GET /api/events` → `200 OK` (only the caller's events)
  - `GET /api/events/{id}` → `200 OK`
  - `PUT /api/events/{id}` → `200 OK`
  - `DELETE /api/events/{id}` → `204 No Content`
- `EventNotFoundException` — generic "Event not found" message, used both
  when an event doesn't exist and when it belongs to another user
- `InvalidEventTimeException` — "Event end time must be after start time"
- `GlobalExceptionHandler` updated to turn `EventNotFoundException` into a
  `404` `ErrorResponse` and `InvalidEventTimeException` into a `400`
  `ErrorResponse`
- `EventServiceTest` (Mockito, no database) covering: create with an owned
  calendar succeeds, create with another user's calendar throws
  `CalendarNotFoundException`, an owned category can be assigned, another
  user's category throws `CategoryNotFoundException`, an end time not after
  the start time throws `InvalidEventTimeException`, list returns only that
  user's events, getting another user's/a missing event throws
  `EventNotFoundException`, update changes supported fields, setting
  `categoryId` to `null` on update clears an existing category, delete calls
  the repository delete operation
- `EventControllerTest` (`@WebMvcTest`, real `SecurityConfig`) covering:
  authenticated create returns `201`, unauthenticated request returns `401`,
  invalid request returns `400`, a missing/not-owned event returns `404`,
  delete returns `204`

**Explicitly not done in Phase 5A (deferred to a later Event phase):**

- Recurrence is stored as-is on the `RecurrenceType` enum column but is never
  expanded — no repeated occurrences or future dates are generated, and no
  extra rows are created for recurring events.
- `reminderOffsetMinutes` is stored and returned but nothing schedules,
  calculates, or delivers a reminder.
- No date-range filtering, calendar/category filtering, or keyword search.
- No drag-and-drop/reschedule `PATCH` endpoint.
- The known limitation from Phase 4A (deleting a `Calendar` that still has
  `Event` rows will now fail with a foreign-key violation, since events can
  finally exist) is still unresolved by design — no cascade/reassignment
  policy has been added yet.

---

## Phase 5B — Event Filtering

Phase 5B completed:

- `GET /api/events` now accepts four optional, independently-usable query
  parameters: `start`, `end` (both `LocalDateTime`, ISO format, e.g.
  `2026-01-01T09:00:00`), `calendarId`, and `categoryId`. Calling it with no
  parameters still returns every event the authenticated user owns, exactly
  as before this phase. No second list endpoint was added.
- All supplied filters combine with **AND** behavior — e.g.
  `?calendarId=3&categoryId=7` returns only events in calendar 3 **and**
  category 7, both still scoped to the authenticated user.
- **Date-range filtering uses overlap, not containment:** when both `start`
  and `end` are supplied, an event matches if `event.startTime < end` AND
  `event.endTime > start`, so an event that began before the visible window
  but is still ongoing when it starts is still included.
- **Date parameter rules**, enforced in `EventService`: neither `start` nor
  `end` supplied → no date filter; both supplied and valid → filter by
  overlap; exactly one supplied → `400 Bad Request`; `end` not after `start`
  → `400 Bad Request`. Both cases reuse the existing
  `InvalidEventTimeException` (message: "Start and end must form a valid
  date range") — no new exception type was introduced.
- **Calendar/Category filter ownership:** if `calendarId` is supplied,
  `EventService` reuses its existing `findOwnedCalendar` helper
  (`CalendarRepository.findByIdAndOwnerId`) to confirm it belongs to the
  authenticated user, returning the existing generic `404` otherwise. The
  same applies to `categoryId` via the existing `findOwnedCategory` helper.
  Neither filter ID is trusted without this check.
- `EventRepository.findByCalendarOwnerId(ownerId)` was replaced by a single
  `@Query`-based method, `findByCalendarOwnerIdAndFilters(ownerId,
  calendarId, categoryId, start, end)`, using plain JPQL with
  `(:param IS NULL OR ...)` clauses so each filter is skipped when not
  supplied — this one method covers every filter combination (including
  none) instead of one derived-query method per combination. The query uses
  a `LEFT JOIN` on `Event.category` specifically so that events with no
  category are not silently excluded by an implicit inner join when no
  category filter is applied. No Specifications, Criteria API, QueryDSL, or
  other filtering framework was introduced.
- `EventServiceTest` extended with: no filters still lists all of the
  authenticated user's events, a date range filters by overlap, supplying
  only one date parameter is rejected, an invalid range (`end` not after
  `start`) is rejected, a calendar filter verifies ownership (and rejects
  another user's calendar), a category filter verifies ownership (and
  rejects another user's category), and combined filters are applied
  together
- `EventControllerTest` extended with: query parameters are correctly parsed
  and passed through to the service, and an invalid filter (one date
  parameter without the other) returns `400`. The existing unauthenticated
  `GET /api/events` test already covers authentication and was not
  duplicated.

**Historical wording cleanup:** the Phase 4A and Phase 4B sections above
originally stated that "Event CRUD does not exist yet" when describing why
certain foreign-key constraints (`events.calendar_id`, `events.category_id`)
were unreachable at the time. Those sentences have been reworded to state
that this described the project **at the end of that specific phase**, since
Event CRUD has existed since Phase 5A and both constraints are reachable
today. No other historical wording was changed.

**Explicitly not done in Phase 5B (deferred to a later Event phase):**

- Recurrence is still stored only, never expanded — a `DAILY`/`WEEKLY`/
  `MONTHLY` event is still exactly one stored row; there is no recurrence
  filtering, occurrence generation, or recurrence end date.
- `reminderOffsetMinutes` remains a stored/returned preference only —
  reminder delivery (email, push, browser notifications, scheduled jobs)
  is intentionally not implemented.
- No keyword search, pagination, sorting framework, or drag-and-drop
  reschedule (`PATCH`) endpoint.
- Calendar/category deletion behavior when events still reference them
  (see Phase 4A/4B) remains undecided.

---

## Phase 5C — Recurring-Event Expansion

Phase 5C completed:

- **One database row per recurring series, still.** A `DAILY`/`WEEKLY`/
  `MONTHLY` `Event` is still exactly one row in `events`; no schema change was
  made and no additional rows are ever created for occurrences. The stored
  `startTime`/`endTime` represent the **first** occurrence, and every
  generated occurrence keeps that same duration
  (`Duration.between(startTime, endTime)`).
- **Expansion only happens for date-range queries.** `EventService.listEvents`
  only expands recurrence when both `start` and `end` are supplied to
  `GET /api/events`. With no date range, every stored event (recurring or
  not) is returned exactly once, unexpanded — the same behavior as before
  this phase. `GET /api/events/{id}` is untouched and always returns the
  single stored definition, never occurrences.
- **`EventService.expandOccurrences(event, rangeStart, rangeEnd)`** — a small
  private helper that starts from the event's stored `startTime` and steps
  forward one interval at a time (`plusDays(1)` for `DAILY`, `plusWeeks(1)`
  for `WEEKLY`, `plusMonths(1)` for `MONTHLY`, using Java's ordinary
  `LocalDateTime` behavior with no custom month-length/leap-year handling),
  keeping any occurrence whose calculated start/end overlaps the requested
  range using the existing overlap rule
  (`occurrenceStart < rangeEnd AND occurrenceEnd > rangeStart`). The loop
  naturally stops as soon as an occurrence's start is no longer before
  `rangeEnd`, so it never generates further into the future than the
  requested range requires. No recurrence engine, strategy pattern, or
  interval configuration was introduced — just a loop and a `switch` on
  `RecurrenceType`.
- **Generated occurrences are `EventResponse` objects only** — nothing is
  saved back to the database, and no occurrence entity exists. The existing
  `EventResponse` is reused unchanged: an occurrence keeps the original
  Event's `id`, `title`, `description`, `location`, `allDay`,
  `recurrenceType`, `reminderOffsetMinutes`, and calendar/category fields,
  with only `startTime`/`endTime` replaced by the generated occurrence's
  values. This means a single filtered `GET /api/events?start=...&end=...`
  response can legitimately contain the same `id` more than once (once per
  occurrence) — the `id` identifies the stored recurring Event/series, not an
  individual occurrence. No occurrence ID was added to the backend in this
  phase; a future frontend can construct its own display identifier (e.g.
  combining the event `id` with its occurrence `startTime`) if it needs one.
- **`EventRepository.findByCalendarOwnerIdAndFilters` updated** so recurring
  events that started before the requested range are still fetched as
  candidates for expansion. The date-range condition now has two cases: a
  `NONE` event still requires its stored `startTime`/`endTime` to overlap the
  range exactly as in Phase 5B; a recurring event (`recurrenceType <> NONE`)
  only requires its stored `startTime` to be before the requested `end`,
  since its occurrences only ever move forward in time and a series that
  began long ago can still have an occurrence inside the requested range.
  Owner filtering, calendar filtering, and category filtering all remain in
  this same JPQL query exactly as before — no Specifications, Criteria API,
  QueryDSL, or native SQL was introduced.
- **Calendar/Category filters continue to work with recurrence** — they still
  narrow which `Event` definitions are fetched from the database (with
  ownership enforced exactly as in Phase 5B); expansion then runs per
  definition afterward, so a calendar- or category-filtered request still
  only expands events that passed those filters.
- **Update/delete meaning is unchanged.** Since a recurring series is one
  row, updating an `Event` updates the whole series' definition (there is no
  "edit this occurrence only"), and deleting it removes the entire series
  (there is no "delete this occurrence only"). No recurrence exceptions or
  skipped-occurrence support exists.
- `EventServiceTest` extended with: `NONE` still returns one overlapping
  occurrence, `DAILY`/`WEEKLY`/`MONTHLY` each generate the expected
  occurrences within a requested range, occurrence duration matches the
  stored event's duration, occurrences outside the requested range are
  excluded, a recurring event whose stored date is well before the requested
  range still produces occurrences inside it, no date range returns the
  recurring event once unexpanded, and combined Calendar/Category filtering
  continues to work. No controller tests were added since the controller's
  API surface did not change.

**Explicitly not done in Phase 5C (intentionally out of scope):**

- No recurrence end date or occurrence count — a recurring series is treated
  as continuing indefinitely; expansion is naturally bounded only by the
  requested date range.
- No custom recurrence intervals (e.g. "every 2 weeks"), no RRULE/iCalendar
  parsing, no recurrence exceptions/excluded dates.
- No "edit this occurrence" / "edit this and future occurrences" / "delete
  this occurrence only" behavior.
- Reminder delivery remains intentionally unimplemented —
  `reminderOffsetMinutes` is still stored and returned only.

---

# Current Project Status

The application currently supports:

- User registration
- User login
- JWT generation
- JWT request authentication (`JwtAuthenticationFilter` +
  `JwtAuthenticationEntryPoint`)
- A protected `GET /api/users/me` endpoint
- Calendar CRUD (`/api/calendars`), scoped to the authenticated user
- Category CRUD (`/api/categories`), scoped to the authenticated user
- Event CRUD (`/api/events`), scoped to the authenticated user through
  Calendar ownership, with Calendar/Category ownership validated whenever an
  event references them
- Optional Event filtering on `GET /api/events` by date range (overlap),
  calendar, and/or category, combined with AND behavior, with ownership
  enforced on every filter ID supplied
- Recurring-event expansion (`DAILY`/`WEEKLY`/`MONTHLY`) for date-range
  `GET /api/events` queries, generated in memory only — one stored `Event`
  row still represents the entire recurring series

Every endpoint other than `POST /api/auth/register`, `POST /api/auth/login`,
and the Swagger/OpenAPI paths now requires a valid Bearer token.

---

# Next Phase

## Phase 5D — Remaining Advanced Event Features

Goals:

- Drag-and-drop reschedule endpoint
- Decide how calendar/category deletion interacts with existing events
- Reminder delivery

---

# Remaining Planned Phases

- **Phase 5D** — Remaining advanced Event features (drag-and-drop
  rescheduling, reminder delivery).
- **Phase 6** — Task CRUD.
- **Phase 7** — Dashboard.
- **Phase 8** — React frontend.
- **Phase 9** — Testing, documentation, polish, screenshots, README
  improvements, and deployment preparation.

---

# Important Project Decisions

These are architectural decisions made earlier in the project and should
**not** change in future chats without an explicit, deliberate discussion:

- PostgreSQL is the production database.
- Flyway owns the database schema.
- Hibernate only validates the schema; it never generates or alters it.
- Email is normalized (trimmed, lowercased) before persistence and before
  lookup.
- Passwords are stored only as BCrypt hashes — the raw password is never
  persisted or logged.
- The JWT subject is the user's email.
- The JWT contains the user's database ID as a custom claim (`userId`).
- `JwtAuthenticationFilter` loads the authenticated user from the database by
  the JWT's email subject claim (`UserRepository.findByEmail`) rather than
  trusting the JWT's claims directly.
- DTOs are used instead of exposing entities directly through the API.
- Controllers remain thin — they delegate to services and do not contain
  business logic.
- Business logic belongs in services.
- Repositories contain only data-access logic.
- No microservices — this is a single Spring Boot application.
- Desktop-only application — no mobile/responsive requirement.
- Keep the project realistic in scope for a recent graduate's portfolio, not
  an enterprise-scale system.

---

# Notes for Future Cursor Chats

This document is the authoritative source for project progress. Future
Cursor chats should:

1. Inspect the repository directly (code, tests, migrations, git history)
   first to confirm the actual current state.
2. Then use this file to understand phase history, current status, and the
   next planned phase before making implementation decisions.

If the repository and this document ever disagree, trust the repository and
update this document to match.
