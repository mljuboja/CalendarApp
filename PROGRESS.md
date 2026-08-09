# Project Progress

This document is the authoritative status document for **Daymark**. It reflects
the current state of the project as of the completion of Phase 8E-6, with
Phase 9 (testing, documentation, and polish) next.

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

### Frontend

- React
- JavaScript
- Vite
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

## Phase 5D — Event Time Update (Drag/Resize) Endpoint

Phase 5D completed:

- `EventTimeUpdateRequest` DTO — only `startTime` and `endTime`, both
  required (`@NotNull`). No other `Event` field can be changed through this
  DTO.
- `PATCH /api/events/{id}/time` (`EventController`) — a small, purpose-built
  endpoint for frontend drag-and-drop/resize actions, returning `200 OK`
  with the existing `EventResponse` on success. This is not a generic PATCH:
  the full `PUT /api/events/{id}` endpoint from Phase 5A is unchanged and
  remains the way to update any other field.
- `EventService.updateEventTime(eventId, request, ownerId)` — reuses the
  existing helpers exactly as they are, with no duplicated logic:
  - `findOwnedEvent` for ownership (same `EventNotFoundException` on a
    missing/not-owned event as every other Event operation)
  - `validateTimes` for the `endTime > startTime` rule (same
    `InvalidEventTimeException` as create/update/filtering)
  - `toResponse` for mapping the saved entity back to `EventResponse`
  - The method only calls `event.setStartTime(...)`/`event.setEndTime(...)`
    before saving — title, description, location, calendar, category,
    recurrence type, reminder offset, and all other fields are left exactly
    as they were.
- **Recurring events:** no recurrence-specific PATCH logic was added or is
  needed. A recurring `Event` is still one stored row (Phase 5C), and
  recurrence expansion always recalculates every occurrence from that row's
  current `startTime`/`endTime`. Changing those two fields through this
  endpoint therefore moves the entire recurring series at once — there is
  still no "move this occurrence only" behavior, exactly as before this
  phase.
- No new repository methods, exceptions, generic PATCH/partial-update
  infrastructure, or custom Bean Validation annotations were introduced.
- `EventServiceTest` extended with: a valid owned-event time update succeeds
  and returns the new times, an invalid range (`endTime` not after
  `startTime`) throws `InvalidEventTimeException`, a missing/not-owned event
  throws `EventNotFoundException`, and non-time fields (title, description,
  location, recurrence type, calendar) are confirmed unchanged after the
  update.
- `EventControllerTest` extended with: an authenticated valid `PATCH`
  returns `200`, a request missing `startTime`/`endTime` returns `400`, and
  a missing/not-owned event returns `404`. No additional unauthenticated
  test was added since the existing `GET /api/events` `401` test already
  proves the `/api/events` endpoint group requires authentication.

**Explicitly not done in Phase 5D (intentionally out of scope):**

- No generic/partial `PATCH` for other `Event` fields.
- No recurrence end dates, custom recurrence intervals, or recurrence
  exceptions (editing/moving a single occurrence).
- No reminder delivery, search, pagination, or Task CRUD.
- Calendar/category deletion behavior when events still reference them (see
  Phase 4A/4B) remains undecided.

---

## Phase 6 — Task CRUD

Phase 6 completed:

- `TaskRequest` DTO (`title`, `description`, `dueDate`, `priority`, `status`)
  — `title` required (`@NotBlank`), `priority`/`status` required (`@NotNull`),
  `description`/`dueDate` optional, matching the existing `Task` entity and
  Flyway schema exactly (no new fields, columns, or constraints introduced).
  The client can never set `owner`, `id`, `createdAt`, or `updatedAt`.
- `TaskResponse` DTO (`id`, `title`, `description`, `dueDate`, `priority`,
  `status` only — no `User`/owner info and no timestamps, consistent with
  `CalendarResponse`/`CategoryResponse`).
- `TaskService` — create, list, get, update, delete, and updateStatus, all
  scoped to the authenticated user, following the same structure as
  `CalendarService`/`CategoryService`:
  - **Ownership:** unlike `Event` (owned indirectly through `Calendar`), a
    `Task` belongs directly to its `owner` column. Every
    get/update/delete/status-update uses the new
    `TaskRepository.findByIdAndOwnerId(id, ownerId)` so a user can never
    touch another user's task.
  - **Create:** the authenticated `User` principal is used directly as the
    owner — it is never reloaded from `UserRepository`.
  - `findOwnedTask(...)` and `toResponse(...)` are small private helpers
    mirroring the existing Calendar/Category/Event pattern; no service
    interface, mapper framework, or generic ownership abstraction was
    introduced.
- `TaskRepository.findByIdAndOwnerId(Long id, Long ownerId)` added alongside
  the existing `findByOwnerId(Long ownerId)`.
- `TaskController` (`/api/tasks`) — thin controller reading the authenticated
  `User` from the `Authentication` parameter; no ownership or database logic
  in the controller itself:
  - `POST /api/tasks` → `201 Created`
  - `GET /api/tasks` → `200 OK` (only the caller's tasks)
  - `GET /api/tasks/{id}` → `200 OK`
  - `PUT /api/tasks/{id}` → `200 OK` (updates `title`, `description`,
    `dueDate`, `priority`, `status`)
  - `DELETE /api/tasks/{id}` → `204 No Content`
  - `PATCH /api/tasks/{id}/status` → `200 OK`
- `TaskStatusUpdateRequest` DTO — only `status` (`@NotNull`). The
  `PATCH /api/tasks/{id}/status` endpoint changes **only** the `status`
  field; `TaskService.updateTaskStatus(...)` finds the owned task, sets the
  new status, saves, and returns the existing `TaskResponse`, leaving title,
  description, due date, and priority untouched. Any of the three
  `TaskStatus` values (`TODO`, `IN_PROGRESS`, `COMPLETED`) can be set
  directly — no transition rules (e.g. requiring `TODO` → `IN_PROGRESS`
  before `COMPLETED`) were added, matching the existing project's decision
  not to add status-transition validation.
- `TaskNotFoundException` — generic "Task not found" message, used both when
  a task doesn't exist and when it belongs to another user.
- `GlobalExceptionHandler` updated to turn `TaskNotFoundException` into a
  `404` `ErrorResponse`, using the same `ErrorResponse` shape as every other
  not-found exception.
- The existing `Priority` (`LOW`/`MEDIUM`/`HIGH`) and `TaskStatus`
  (`TODO`/`IN_PROGRESS`/`COMPLETED`) enums are used as-is; no duplicate enum
  types or custom enum converters were introduced — Jackson serializes/
  deserializes them as plain strings the normal way.
- `TaskServiceTest` (Mockito, no database) covering: create assigns the
  authenticated user as owner, list uses the authenticated user's ID,
  getting an owned task succeeds, getting a missing/not-owned task throws
  `TaskNotFoundException`, update changes the editable fields, delete calls
  the repository delete operation, and a status-only update changes the
  status while leaving title/description/priority unchanged.
- `TaskControllerTest` (`@WebMvcTest`, real `SecurityConfig`) covering:
  authenticated create returns `201`, unauthenticated request returns `401`,
  invalid request returns `400`, a missing task returns `404`, the status
  `PATCH` returns `200`, and delete returns `204`.

**Explicitly not done in Phase 6 (intentionally out of scope):**

- No Task filtering, search, pagination, or sorting framework.
- No recurring Tasks or Task notifications.
- No dashboard or productivity-analytics behavior.
- No frontend work.
- No roles, permissions, or Task sharing between users.

---

## Phase 7 — Dashboard

Phase 7 completed:

- `GET /api/dashboard` — a single endpoint that combines existing Event and
  Task data into one summary response for the authenticated user. This is
  not a general analytics system: it only reuses `EventService`/`TaskService`
  and adds one small calculation (today's scheduled hours).
- `DashboardResponse` DTO — `todaysEvents` (`List<EventResponse>`),
  `upcomingTasks` (`List<TaskResponse>`), `completedTaskCount` (`long`),
  `scheduledHoursToday` (`double`). No productivity score, streaks,
  percentages, charts, or trend data.
- **Today's Events reuse `EventService.listEvents(...)` unchanged** —
  `DashboardService` calls
  `eventService.listEvents(ownerId, todayStart, tomorrowStart, null, null)`,
  the exact same public method `GET /api/events` already uses. This means
  the Phase 5B date-overlap filtering and Phase 5C recurrence expansion are
  reused as-is: a `WEEKLY` event created months ago that has an occurrence
  today is already included, with no recurrence code duplicated in
  `DashboardService`. `EventService` itself was not modified.
- **"Today" is plain `LocalDateTime`, no time zones** — `todayStart` is
  `LocalDate.now().atStartOfDay()` and `tomorrowStart` is
  `todayStart.plusDays(1)`, using the server's local time exactly like the
  rest of the project. No `ZoneId`, UTC conversion, or per-user time zone
  was introduced.
- **Upcoming Tasks** — `TaskRepository.findUpcomingTasks(ownerId,
  completedStatus, today)`, a short JPQL `@Query` (not a long derived method
  name) that returns the user's Tasks where `status <> COMPLETED` and
  `dueDate >= :today`, ordered by `dueDate` ascending. A `null` `dueDate`
  is naturally excluded by the `>=` comparison — no extra null-check code
  needed. No arbitrary 7/30-day cutoff.
- **Completed Task count** — `TaskRepository.countByOwnerIdAndStatus(ownerId,
  COMPLETED)`, a simple derived count query; every Task is never loaded into
  Java just to count them.
- `TaskService` extended with two small methods that reuse the existing
  private `toResponse(...)` mapping: `getUpcomingTasks(Long ownerId)` and
  `getCompletedTaskCount(Long ownerId)`.
- **Scheduled hours today** — a small private `calculateScheduledHours(...)`
  helper in `DashboardService` sums, for every `EventResponse` already
  returned as "today's Events," the portion of that event that falls inside
  `[todayStart, tomorrowStart)`: `effectiveStart` is the later of the
  event's start and `todayStart`; `effectiveEnd` is the earlier of the
  event's end and `tomorrowStart`. `Duration.between(...)` gives the
  overlapping minutes, which are summed and divided by `60.0`. This
  correctly clips an event that starts yesterday and ends today (or starts
  today and ends tomorrow) to only the portion that's actually today. No
  analytics utility, `BigDecimal`, or custom duration class was introduced.
- `DashboardService` — a small, non-interface service with one public
  method (`getDashboard(Long ownerId)`) and one private helper, depending
  only on the existing `EventService` and `TaskService` (no direct
  repository access, no new ownership logic).
- `DashboardController` (`/api/dashboard`) — a single thin
  `GET /api/dashboard` method reading the authenticated `User` from
  `Authentication`, exactly like every other controller; no calculations or
  repository calls in the controller itself.
- `DashboardServiceTest` (Mockito, `EventService`/`TaskService` mocked)
  covering: the response includes today's Events/upcoming Tasks/completed
  count from the mocked services, scheduled hours sum correctly for events
  fully inside today, and scheduled hours correctly clip an event that
  crosses into yesterday and one that crosses into tomorrow. Recurrence and
  date-filtering behavior itself is not re-tested here since it's already
  covered by `EventServiceTest`.
- `DashboardControllerTest` (`@WebMvcTest`, real `SecurityConfig`) covering:
  an authenticated `GET /api/dashboard` returns `200` with all four expected
  JSON sections present, and an unauthenticated request returns `401`.

**Explicitly not done in Phase 7 (intentionally out of scope):**

- No productivity scores, streaks, weekly/monthly analytics, charts, or
  trend calculations.
- No Task search, Event search, or pagination.
- No reminder delivery, notifications, or scheduled jobs.
- No frontend work.
- No Specifications, Criteria API, QueryDSL, or generic query builders.

---

## Phase 8A — React Frontend Foundation

Phase 8A completed:

- The React/Vite frontend foundation, in `frontend/`, alongside the existing
  Spring Boot project at the repository root (the Spring Boot project itself
  was not moved or restructured).
- **Plain JavaScript**, not TypeScript — `.jsx` files, no `tsconfig.json`. The
  frontend's "planned" tech stack listed earlier in this document mentioned
  TypeScript; that was an early aspiration and has not been followed, since
  the actual repository never established TypeScript.
- React Router (`react-router-dom`) configured with placeholder routes:
  `/login` → `LoginPage`, `/register` → `RegisterPage`, and `/`, `/calendar`,
  `/tasks` → `DashboardPage`/`CalendarPage`/`TasksPage`, the latter three
  rendered through a shared `AppLayout` (`<Outlet />`). Login/Register do not
  use `AppLayout`.
- A basic desktop `AppLayout` (`frontend/src/layouts/AppLayout.jsx`) with a
  "Daymark" header, simple navigation links to Dashboard/Calendar/Tasks, and
  an `<Outlet />` — no sidebar, no polished visual design yet.
- A single centralized Axios instance (`frontend/src/api/apiClient.js`) that
  only creates and exports `axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL })`.
  No JWT/`localStorage` reading, no `Authorization` header, no interceptors,
  and no other authentication behavior — those are intentionally deferred to
  Phase 8B.
- `frontend/.env.example` documenting `VITE_API_BASE_URL=http://localhost:8080`
  (the backend's actual default port); a local `frontend/.env` with the same
  value exists for development and is git-ignored.
- Small, neutral global CSS (`frontend/src/index.css`) replacing the default
  Vite/React template styling — no CSS framework, animations, dark mode, or
  responsive/mobile work.
- Unused Vite starter template files removed (`App.css`, `assets/react.svg`,
  `assets/vite.svg`, `assets/hero.png`, `public/icons.svg`) after confirming
  they were not referenced anywhere in the frontend once the placeholder
  pages replaced the default template page.
- All five placeholder pages (`DashboardPage`, `CalendarPage`, `TasksPage`,
  `LoginPage`, `RegisterPage`) contain only a heading and a one-line
  description — no forms, no API calls, no feature UI.

**Explicitly not done in Phase 8A (deferred to Phase 8B):**

- No JWT/`localStorage` handling, Axios interceptor, authentication context,
  protected routes, login/registration forms, or logout.
- No real API integration — nothing in the frontend calls the backend yet.
- No backend changes — no Java files were modified, and CORS was
  deliberately **not** configured yet, since Phase 8A makes no browser
  requests to the backend that would trigger it. CORS configuration is
  deferred to Phase 8B, the first phase with real `axios` calls from the
  browser to Spring Boot.
- No Dashboard/Calendar/Task feature UI, drag-and-drop, or recurrence
  controls.
- No frontend testing framework (Vitest/Jest/React Testing Library).

---

## Phase 8B — Frontend Authentication

Phase 8B completed:

- `LoginPage` connected to `POST /api/auth/login` via the existing
  `apiClient` — sends `email`/`password`, using the same `useState` form
  fields introduced when the form was first built.
- `RegisterPage` connected to `POST /api/auth/register` via `apiClient` —
  sends `firstName`/`lastName`/`email`/`password`.
- Both forms have a simple `isLoading` state (disables the submit button and
  swaps its label while the request is in flight) and a simple
  `errorMessage` state (shows the backend's `ErrorResponse.message` when
  present, otherwise a generic "Something went wrong").
- On successful login, the JWT (`response.data.token`) is saved to
  `localStorage` under the key `authToken`, and the user is navigated to
  `/`. On successful registration, the user is navigated to `/login` (no
  token is issued by registration).
- `apiClient.js` — one Axios request interceptor reads `authToken` from
  `localStorage` on every outgoing request and, if present, sets
  `Authorization: Bearer <token>`; otherwise the request is left unchanged.
- `ProtectedRoute` (`frontend/src/components/ProtectedRoute.jsx`) — reads
  `authToken` from `localStorage` and either renders `<Outlet />` or
  redirects to `/login` via `<Navigate replace />`. It only checks whether a
  token exists; it does not decode or validate it client-side.
- Routing updated so `/`, `/calendar`, and `/tasks` are nested under
  `ProtectedRoute` (which wraps the existing `AppLayout`), so all three
  require a token and unauthenticated visitors are redirected to `/login`.
  `/login` and `/register` remain outside `ProtectedRoute` and stay public.
- `AppLayout` — added a "Log Out" button that removes `authToken` from
  `localStorage` and navigates to `/login`.
- `npm run build` passes with no errors after every change in this phase.

**Explicitly not done in Phase 8B (deferred to later phases):**

- No auth context, no decoding/validation of the JWT on the client, and no
  token expiration checks — `ProtectedRoute` only checks whether a token
  string exists in `localStorage`.
- No automatic 401 handling (e.g. auto-logout on an expired/rejected token),
  no refresh tokens, and no roles/permissions.
- No redirect-away logic if an already-logged-in user manually visits
  `/login` or `/register`.
- No user profile fetching (`GET /api/users/me` is not called from the
  frontend yet).
- No Dashboard/Calendar/Task feature UI — those pages are still placeholders.

**Known gap at the end of Phase 8B (resolved after Phase 8C — see below):**
at the time Phase 8B was written, the frontend made real browser requests to
the backend (login/register) from `http://localhost:5173` to
`http://localhost:8080` with no CORS configuration in the backend, so these
cross-origin requests would have failed in an actual browser. CORS was
configured immediately after Phase 8C (see "CORS Configuration" below), so
this is no longer an open gap.

---

## Phase 8C — Dashboard Frontend

Phase 8C completed:

- `DashboardPage` now calls `GET /api/dashboard` through the existing
  `apiClient`, fetched once on page load with a plain `useEffect`/`useState`
  pair (no data-fetching library).
- Simple `isLoading` state shows "Loading..." while the request is in
  flight, and a simple `errorMessage` state shows the backend's
  `ErrorResponse.message` (or a generic "Something went wrong") if the
  request fails.
- All four pieces of `DashboardResponse` are displayed:
  - **Today's Events** — each shown as title plus start/end time
    (`event.title`, `event.startTime`, `event.endTime`); "No events today"
    when the list is empty.
  - **Upcoming Tasks** — each shown as title, due date, priority, and status
    (`task.title`, `task.dueDate`, `task.priority`, `task.status`); "No
    upcoming tasks" when the list is empty.
  - **Completed Tasks** — `completedTaskCount` shown as a plain number.
  - **Scheduled Hours Today** — `scheduledHoursToday` shown as a plain
    number.
- A little plain CSS (`.dashboard-section`, `.dashboard-list`) was added to
  `index.css` for basic spacing — no charts, calendar libraries, or new
  component abstractions.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8C (intentionally out of scope):**

- No task/event editing from the Dashboard, no filtering, no charts, and no
  calendar UI library.
- No auth context or automatic 401 handling — if the JWT is missing/invalid,
  the request simply fails and the existing generic error message is shown.
- No backend changes.

---

## CORS Configuration (local Vite ↔ Spring Boot)

Immediately after Phase 8C, standard Spring Security CORS support was added
to `SecurityConfig` so the frontend's real API calls (login, register,
dashboard) can actually succeed from a browser:

- A `CorsConfigurationSource` bean only allows the local Vite dev server's
  exact origin, `http://localhost:5173` — no wildcard (`*`) origin.
- Allowed methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE` (everything this
  API currently uses).
- Allowed headers: `Authorization` and `Content-Type` (everything the
  frontend currently sends).
- `.cors(cors -> cors.configurationSource(corsConfigurationSource()))` was
  added to the existing `SecurityFilterChain` alongside the existing
  `.csrf()`/`.sessionManagement()`/etc. calls. No other line in
  `SecurityFilterChain` changed — `authorizeHttpRequests`, the JWT filter,
  and the JWT entry point are all unchanged, so authentication/authorization
  behavior is identical to before.
- No new CORS framework, filter, or `@CrossOrigin` annotations were
  introduced — this is Spring Security's standard, built-in CORS support.
- `mvn test` passes with the same 107 tests, 0 failures, confirming no
  existing security/auth behavior regressed.

This was the one backend change made specifically to support the frontend
work in Phase 8A–8C; no other backend code was modified for those phases.

---

## Phase 8D-1 — Task List Display

Phase 8D-1 completed:

- `TasksPage` now calls `GET /api/tasks` through the existing `apiClient`,
  fetched once on page load with a plain `useEffect`/`useState` pair — the
  same pattern already used on `DashboardPage`. No separate Task API
  module, custom hook, or data-fetching library was introduced.
- Simple `isLoading` state shows "Loading..." while the request is in
  flight, and a simple `errorMessage` state shows the backend's
  `ErrorResponse.message` (or a generic "Something went wrong") if the
  request fails.
- Each task in the returned list (`TaskResponse`) is displayed with its
  title, description (only if present), due date (only if present),
  priority, and status.
- "No tasks yet." is shown when the list is empty.
- A little plain CSS (`.task-list`, `.task-item`, `.task-title`,
  `.task-description`, `.task-meta`) was added to `index.css` for basic,
  readable spacing consistent with the existing site — no new component
  abstractions.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8D-1 (intentionally out of scope):**

- No creating, editing, deleting, or status-changing tasks from the UI yet.
- No filtering, sorting, or pagination.
- No backend changes.

---

## Phase 8D-2 — Task Creation

Phase 8D-2 completed:

- `TasksPage` now has a simple "Add a Task" form above the task list, with
  controlled inputs (`useState`) matching the backend `TaskRequest` exactly:
  `title` (text), `description` (text), `dueDate` (date input, optional),
  `priority` (`<select>`: `LOW`/`MEDIUM`/`HIGH`), and `status` (`<select>`:
  `TODO`/`IN_PROGRESS`/`COMPLETED`).
- On submit, the form calls `POST /api/tasks` through the existing
  `apiClient` with the current form values (`dueDate` sent as `null` when
  left blank).
- On a successful create, the returned `TaskResponse` is appended to the
  existing `tasks` state (no full re-fetch) and the form fields are reset to
  their defaults.
- A separate `isSubmitting` state disables the "Add Task" button and swaps
  its label to "Adding..." while the request is in flight, independent of
  the page's initial `isLoading` state used for the task list fetch.
- A separate `createErrorMessage` state shows the backend's
  `ErrorResponse.message` (or a generic "Something went wrong") under the
  form if creation fails, without disturbing the existing task list or its
  own error state.
- Plain CSS (`.task-form` and related rules) was added to `index.css`,
  styled consistently with the existing `.auth-form` — no new libraries or
  reusable form abstractions.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8D-2 (intentionally out of scope):**

- No editing, deleting, or status-changing actions on existing tasks yet.
- No client-side validation beyond the browser's native `<input>`/`<select>`
  behavior — invalid input is surfaced only through the existing backend
  error-message display.
- No backend changes.

---

## Phase 8D-3 — Task Status Updates

Phase 8D-3 completed:

- Each task in the `TasksPage` list now has a status `<select>`
  (`TODO`/`IN_PROGRESS`/`COMPLETED`) reflecting `task.status`.
- Changing the select calls `PATCH /api/tasks/{id}/status` through the
  existing `apiClient` with `{ status: <selected value> }`, reusing the
  existing backend endpoint from Phase 6 as-is.
- On a successful response, the returned `TaskResponse` replaces that one
  task in the existing `tasks` array (`tasks.map(...)`, matching by `id`) —
  no full re-fetch of the task list.
- A single shared `statusErrorMessage` state shows the backend's
  `ErrorResponse.message` (or a generic "Something went wrong") near the
  task list if a status update fails; it does not affect the task-creation
  form's own error state or the list's initial-load error state.
- No per-task loading state, disabling, or optimistic-update/rollback
  behavior was added — every status `<select>` stays usable at all times,
  including while another task's status update is in flight, exactly as
  scoped.
- Plain CSS (`.task-status-select`) was added to `index.css`, and
  `.task-meta` was updated to vertically align the select with the existing
  text.
- The existing task-creation form and task list display continue to work
  unchanged.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8D-3 (intentionally out of scope):**

- No task editing, deleting, filtering, sorting, or search.
- No drag-and-drop.
- No custom hooks, reducer pattern, or state-management library — status
  updates are handled with the same `useState`/`apiClient` pattern already
  used for creating and listing tasks.
- No backend changes.

---

## Phase 8D-4 — Task Edit and Delete

Phase 8D-4 completed:

- Each task in the `TasksPage` list now has **Edit** and **Delete** buttons
  next to the existing status `<select>`.
- **Edit:** clicking Edit tracks a single `editingTaskId` plus a small set
  of `useState` fields (`editTitle`, `editDescription`, `editDueDate`,
  `editPriority`, `editStatus`) pre-filled from that task. Only one task can
  be in edit mode at a time — clicking Edit on a different task just moves
  `editingTaskId` (no per-task edit-state map or reusable editing
  framework). While a task is being edited, its list item conditionally
  renders an inline form instead of its normal display.
- **Save:** calls `PUT /api/tasks/{id}` through the existing `apiClient`
  with the edited values, replaces that task in the existing `tasks` array
  with the returned `TaskResponse` (`tasks.map(...)` by `id`, no re-fetch),
  and exits edit mode.
- **Cancel:** simply clears `editingTaskId` without calling the backend or
  changing the task's data.
- **Delete:** asks for confirmation with the browser's built-in
  `window.confirm(...)` (no confirmation-modal library), then calls
  `DELETE /api/tasks/{id}` and removes that task from the existing `tasks`
  array (`tasks.filter(...)`, no re-fetch) on success.
- A single shared `editErrorMessage` state shows the backend's
  `ErrorResponse.message` (or a generic "Something went wrong") near the
  task list if an edit-save or delete fails, separate from the existing
  create-form and status-update error states.
- Plain CSS was added for the Edit/Delete buttons, the inline edit form's
  inputs/selects, and the Save/Cancel button row — no modal library, no new
  component abstractions.
- The existing task-creation form, task list display, and status `<select>`
  all continue to work unchanged.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8D-4 (intentionally out of scope):**

- No filtering, sorting, search, drag-and-drop, or batch actions.
- No custom hooks, reducer pattern, modal library, form library, or
  separate Task API module.
- No backend changes.

---

## Phase 8E-1 — Event List Display

Phase 8E-1 completed:

- `CalendarPage` now calls `GET /api/events` (no query parameters) through
  the existing `apiClient`, fetched once on page load with a plain
  `useEffect`/`useState` pair — the same pattern already used on
  `DashboardPage`/`TasksPage`. No separate Event API module, custom hook, or
  data-fetching library was introduced.
- Simple `isLoading` state shows "Loading..." while the request is in
  flight, and a simple `errorMessage` state shows the backend's
  `ErrorResponse.message` (or a generic "Something went wrong") if the
  request fails.
- "No events yet." is shown when the list is empty.
- Each event in the returned list (`EventResponse`) is displayed using the
  real backend field names: `title`, `startTime`/`endTime` (formatted with
  the built-in `new Date(...).toLocaleString()` — no date library),
  `description` (only if present), `location` (only if present),
  `calendarName`, `categoryName` (only if present), and `recurrenceType`.
- No frontend recurrence expansion was added — calling `GET /api/events`
  with no date range returns each recurring event's stored definition once,
  exactly as the backend already documents, and the page simply displays
  whatever the endpoint returns.
- A little plain CSS (`.event-list`, `.event-item`, `.event-title`,
  `.event-description`, `.event-meta`) was added to `index.css`, styled
  consistently with the existing `.task-list`/`.task-item` rules — no new
  component abstractions.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8E-1 (intentionally out of scope):**

- No event creation, editing, or deletion.
- No drag-and-drop, resizing, month/week/day views, date navigation, or any
  calendar UI library.
- No event/calendar/category filtering.
- No Calendar or Category CRUD from the UI.
- No date library — formatting uses only `Date.prototype.toLocaleString()`.
- No backend changes.

---

## Phase 8E-2 — Event Creation

Phase 8E-2 completed:

- `CalendarPage` now has a simple "Add an Event" form above the event list,
  with controlled inputs (`useState`) matching the backend `EventRequest`
  exactly: `title` (text), `description` (text), `location` (text),
  `startTime`/`endTime` (`datetime-local` inputs), `allDay` (checkbox),
  `recurrenceType` (`<select>`: `NONE`/`DAILY`/`WEEKLY`/`MONTHLY`),
  `reminderOffsetMinutes` (number input, optional — sent as `null` when
  left blank), `calendarId` (`<select>`, required), and `categoryId`
  (`<select>`, optional).
- On page load, in addition to the existing events fetch, `CalendarPage`
  fetches `GET /api/calendars` and `GET /api/categories` via plain
  `useEffect` calls (all three requests fire together, no request
  chaining/waterfall) and stores the results in `calendars`/`categories`
  state to populate the two `<select>` elements. The calendar select
  defaults to the user's first calendar; the category select has a
  "No category" option that sends `categoryId: null`.
- On submit, calls `POST /api/events` through the existing `apiClient` with
  the form values (`calendarId`/`categoryId` converted to numbers since
  `<select>` values are always strings). On success, the returned
  `EventResponse` is appended to the existing `events` state (no full
  re-fetch) and the form fields are reset (the calendar selection is left
  as-is rather than cleared, since a calendar is always required).
- A separate `isSubmitting` state disables the "Add Event" button and swaps
  its label to "Adding..." while the request is in flight, and a separate
  `createErrorMessage` state shows the backend's `ErrorResponse.message` (or
  a generic "Something went wrong") under the form if creation fails —
  independent of the page's initial load error state.
- If the user has no calendars, the form is hidden entirely and replaced
  with "Create a calendar before adding an event." (no Calendar CRUD was
  built to resolve this — that remains out of scope).
- Plain CSS (`.event-form` and related rules) was added to `index.css`,
  styled consistently with the existing `.task-form`/`.auth-form` — no new
  libraries or reusable form abstractions.
- The existing event list display continues to work unchanged.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8E-2 (intentionally out of scope):**

- No event editing or deletion.
- No drag-and-drop, resizing, month/week/day views, or date navigation.
- No event/calendar/category filtering.
- No Calendar or Category CRUD from the UI (a missing calendar only shows a
  message; it is not created from this page).
- No calendar/date library — recurrence is a plain `<select>` and
  start/end times use the browser's native `datetime-local` input.
- No backend changes.

---

## Phase 8E-3 — Event Edit and Delete

Phase 8E-3 completed:

- Each event in `CalendarPage`'s list now has "Edit" and "Delete" buttons.
- Clicking "Edit" swaps that single event's list item for an inline edit
  form (same pattern as `TasksPage`'s task edit), tracked with an
  `editingEventId` state plus a separate set of plain `useState` fields
  (`editTitle`, `editDescription`, `editLocation`, `editStartTime`,
  `editEndTime`, `editAllDay`, `editRecurrenceType`,
  `editReminderOffsetMinutes`, `editCalendarId`, `editCategoryId`),
  prefilled directly from that event's current `EventResponse` values (with
  `startTime`/`endTime` trimmed to the `datetime-local` input's expected
  `YYYY-MM-DDTHH:mm` shape). The edit form's calendar/category `<select>`
  options reuse the `calendars`/`categories` state already loaded for the
  create form — they are not re-fetched.
- "Save" calls `PUT /api/events/{id}` with the edited fields in the same
  shape as `EventRequest`, replaces that event in the existing `events`
  state with the returned `EventResponse` (via `map`, no re-fetch), and
  exits edit mode. "Cancel" exits edit mode without saving.
- "Delete" asks for confirmation with `window.confirm()`, then calls
  `DELETE /api/events/{id}` and removes that event from the existing
  `events` state (via `filter`, no re-fetch).
- A single `eventActionErrorMessage` state shows one shared error message
  near the event list for either a failed edit or a failed delete, kept
  separate from the create form's own `createErrorMessage`.
- Only one event can be edited at a time; no modal or reusable event-form
  component was introduced. Editing a recurring event updates the single
  stored series definition — there is no "edit this occurrence only"
  behavior.
- The existing event creation form and event list continue to work
  unchanged.
- Plain CSS (`.event-meta button`, `.event-item label/input/select`,
  `.event-edit-actions`) was added to `index.css`, mirroring the existing
  task list edit styles.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8E-3 (intentionally out of scope):**

- No drag-and-drop or resizing.
- No month/week/day calendar views.
- No event/calendar/category filtering.
- No Calendar or Category CRUD from the UI.
- No recurrence calculations or occurrence-specific editing.
- No backend changes.

---

## Phase 8E-4 — Calendar and Category Management

Phase 8E-4 completed:

- `CalendarPage` now has a "Calendars" section and a "Categories" section
  (each a plain bordered list showing a color swatch, the name, and Edit/
  Delete buttons), placed below the existing event list.
- Calendar create: a small form (`name` text input, `color` using
  `<input type="color">`) calls `POST /api/calendars` and appends the
  returned `CalendarResponse` to the existing `calendars` state.
- Calendar edit: clicking "Edit" swaps that single list item for an inline
  form (tracked with `editingCalendarId` plus plain `editCalendarNameValue`/
  `editCalendarColorValue` state, prefilled from the calendar), "Save" calls
  `PUT /api/calendars/{id}` and replaces that calendar in state via `map`,
  "Cancel" exits edit mode without saving. Only one calendar can be edited
  at a time.
- Calendar delete: `window.confirm()` then `DELETE /api/calendars/{id}`,
  removing it from state via `filter` on success.
- Category create/edit/delete work identically to Calendars, using their
  own state (`newCategoryName`/`newCategoryColor`, `editingCategoryId`,
  `editCategoryNameValue`/`editCategoryColorValue`) and their own endpoints
  (`POST`/`PUT`/`DELETE /api/categories`(`/{id}`)).
- A `calendarActionErrorMessage` and a separate `categoryActionErrorMessage`
  show one shared error message per section for any failed create/edit/
  delete — including the backend's own message if a delete is blocked
  because existing Events still reference that Calendar/Category (no
  frontend reassignment logic was added; the backend's current
  foreign-key/validation behavior was not changed).
- No new fetches were needed for this: the event creation form's and event
  edit form's calendar/category `<select>` elements read from the same
  `calendars`/`categories` state that the new management sections update
  directly, so creating, editing, or deleting a Calendar/Category is
  immediately reflected in the event forms.
- The existing event creation, edit, and delete functionality, and the
  event list, continue to work unchanged.
- Plain CSS (`.management-section`, `.management-form`, `.management-list`,
  `.management-item`, `.color-swatch`, `.management-actions`) was added to
  `index.css`.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8E-4 (intentionally out of scope):**

- No visual month/week/day calendar, drag-and-drop, or event resizing.
- No event filtering UI or recurrence UI changes.
- No Calendar or Category sharing.
- No frontend logic to reassign/handle Events when a delete is blocked —
  the backend's existing error message (or a generic fallback) is simply
  displayed.
- No backend changes.

---

## Phase 8E-5 — Visual Calendar View

Phase 8E-5 completed:

- Added the official FullCalendar React packages:
  `@fullcalendar/react`, `@fullcalendar/core`, `@fullcalendar/daygrid` (no
  interaction/drag-and-drop plugins).
- `CalendarPage` now renders a `<FullCalendar>` month view
  (`initialView="dayGridMonth"`, `dayGridPlugin` only) above the existing
  "Add an Event" form and event list.
- The calendar's `datesSet` callback (fired whenever the visible month
  changes, including on initial render) reads the visible range's
  `startStr`/`endStr` and calls the existing
  `GET /api/events?start=...&end=...`, which the backend already expands
  into individual recurring occurrences for that date range — no
  recurrence math was added on the frontend.
- The returned `EventResponse` list is stored in a new `visibleEvents`
  state and mapped directly to FullCalendar's expected shape
  (`id`, `title`, `start`, `end`, `color`) inline in the `datesSet`
  handler — no reusable mapping layer or adapter was created. Since
  recurring occurrences share the same backend `id`, each mapped event's
  `id` is `${event.id}-${event.startTime}` to keep FullCalendar's client-
  side ids unique; backend ids are untouched. `color` uses the event's
  `categoryColor` if present, otherwise falls back to the matching
  calendar's `color` (looked up from the already-loaded `calendars`
  state).
- A separate `visibleEventsErrorMessage` shows a simple error if the
  range fetch fails, independent of the page's other error states.
- Clicking a calendar event does nothing yet (no edit modal); no date-
  click creation, filtering controls, or view switcher were added.
- The existing event list, event create/edit/delete forms, and Calendar/
  Category management sections all continue to work unchanged and
  independently of the new visual calendar's own data fetch.
- Minimal CSS (`.visual-calendar`) was added to `index.css` just to size
  and space the calendar on the page; FullCalendar v6 ships its own
  default styles so no CSS files needed to be imported separately.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8E-5 (intentionally out of scope):**

- No week or day view, and no view switcher.
- No drag-and-drop or event resizing (no interaction plugin installed).
- No event-click editing or date-click creation.
- No filtering controls.
- No recurrence calculations in React — the backend's existing expansion
  is reused as-is.
- No new backend endpoints or backend changes.

---

## Phase 8E-6 — Calendar Drag and Resize

Phase 8E-6 completed:

- Added `@fullcalendar/interaction` and included `interactionPlugin`
  alongside `dayGridPlugin` in the `<FullCalendar>` `plugins` prop.
- `editable={true}` is set on `<FullCalendar>`, enabling drag-to-move and
  resize-to-change-duration on calendar events.
- When mapping each `EventResponse` occurrence to a FullCalendar event
  object (in the existing `datesSet` handler), the real backend `Event` id
  is now stored in `extendedProps: { eventId: event.id }`, separate from
  the display `id` (`${event.id}-${event.startTime}`) used to keep
  recurring occurrences unique on the client — backend ids are never
  parsed out of that string.
- A single `handleEventChange` handler is wired to both `eventDrop` and
  `eventResize` (FullCalendar passes both callbacks the same `info.event`/
  `info.revert()` shape, so one handler covers both straightforwardly). It
  reads `info.event.extendedProps.eventId`, converts the event's new
  `start`/`end` to the backend's expected format with a small local
  `formatDateForApi` helper (uses the `Date` object's local time fields
  with manual zero-padding — no date library), and calls
  `PATCH /api/events/{id}/time` with a body containing only `startTime`
  and `endTime`, matching `EventTimeUpdateRequest` exactly.
- On success, the moved/resized event is updated in both the `events`
  state (matched by backend id, same `map` pattern used by the existing
  edit forms) and the `visibleEvents` state (matched by the FullCalendar
  display id, so the calendar keeps showing the new position without a
  refetch).
- On failure, a `visibleEventsErrorMessage` shows the backend's message
  (or a generic fallback) and `info.revert()` is called to snap the event
  back to its original position/size — FullCalendar's own built-in
  mechanism, not a custom rollback system.
- Dragging or resizing a recurring occurrence simply calls the same
  `PATCH /api/events/{id}/time` endpoint with the shared backend Event id,
  which updates the whole stored series per existing backend behavior — no
  "edit one occurrence" or recurrence-exception logic was added on the
  frontend.
- The existing visual month view, event list, event create/edit/delete
  forms, and Calendar/Category management sections were not changed and
  continue to work as before.
- `npm run build` passes with no errors.

**Explicitly not done in Phase 8E-6 (intentionally out of scope):**

- No week/day views or view switcher.
- No event-click editing or date-click creation.
- No filtering controls or recurrence-specific UI.
- No custom rollback/state-management framework — `info.revert()` is used
  directly.
- No backend changes.

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
- A dedicated `PATCH /api/events/{id}/time` endpoint for updating only an
  Event's `startTime`/`endTime` (frontend drag-and-drop/resize), which moves
  the whole series for a recurring event since recurrence expansion always
  reads the stored row's current times
- Task CRUD (`/api/tasks`), scoped directly to the authenticated user's
  `owner` column, plus a dedicated `PATCH /api/tasks/{id}/status` endpoint
  for changing only a task's status
- `GET /api/dashboard` — a summary endpoint combining today's Events,
  upcoming incomplete Tasks, completed Task count, and today's scheduled
  Event hours for the authenticated user, built entirely by reusing
  `EventService`/`TaskService`

Every endpoint other than `POST /api/auth/register`, `POST /api/auth/login`,
and the Swagger/OpenAPI paths now requires a valid Bearer token.

On the frontend, the React/Vite app (`frontend/`) now has a working
authentication flow: `LoginPage`/`RegisterPage` call the real backend
endpoints, a JWT is stored in `localStorage` as `authToken`, an Axios
interceptor attaches it to every outgoing request, and `/`, `/calendar`, and
`/tasks` are protected by `ProtectedRoute` (redirecting unauthenticated
visitors to `/login`), with a Logout button that clears the token.
`DashboardPage` is now connected to the real `GET /api/dashboard` endpoint
and displays today's Events, upcoming Tasks, completed Task count, and
scheduled hours today. `TasksPage` now displays the authenticated user's
real tasks (title, description, due date, priority, status) from
`GET /api/tasks`, and supports the full set of task actions from the UI:
creating a task (`POST /api/tasks`), changing a task's status from a
per-task `<select>` (`PATCH /api/tasks/{id}/status`), editing a task inline
(`PUT /api/tasks/{id}`), and deleting a task with a confirmation prompt
(`DELETE /api/tasks/{id}`). `CalendarPage` now displays the authenticated
user's real events (title, start/end time, description, location, calendar
name, category name, recurrence type) from the unfiltered `GET /api/events`
and supports the full set of event actions from the UI: creating an event
(`POST /api/events`, with calendar/category `<select>` options loaded from
`GET /api/calendars`/`GET /api/categories`), editing an event inline
(`PUT /api/events/{id}`), and deleting an event with a confirmation prompt
(`DELETE /api/events/{id}`). `CalendarPage` also now has full Calendar and
Category management sections (create, inline edit, and delete with
confirmation, hitting `/api/calendars` and `/api/categories`), which the
event forms' `<select>` options automatically reflect since they share the
same state. `CalendarPage` additionally renders a FullCalendar month view
that re-fetches `GET /api/events` with the visible date range whenever the
month changes, relying on the backend's existing recurring-event expansion
rather than any client-side recurrence logic, and now supports dragging
and resizing events directly on that calendar (`PATCH /api/events/{id}/time`
via `eventDrop`/`eventResize`, with `info.revert()` on failure). The
backend now has standard Spring Security CORS
configuration allowing the local Vite dev server
(`http://localhost:5173`), so the frontend's real API calls (login,
register, dashboard, tasks, events) can succeed from an actual browser.

---

# Next Phase

## Phase 9 — Testing, Documentation, and Polish

Still undecided/deferred (not tied to a specific upcoming phase yet):

- How calendar/category deletion interacts with existing events beyond
  surfacing the backend's existing error (deferred since Phase 4A/4B)
- Reminder delivery — `reminderOffsetMinutes` remains stored/returned only
- Task filtering/search/sorting, if a future phase decides it's needed

---

# Remaining Planned Phases

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
