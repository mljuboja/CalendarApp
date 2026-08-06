# Project Progress

This document is the authoritative status document for **Daymark**. It reflects
the current state of the project as of the completion of Phase 4A.

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
`ON DELETE CASCADE`. Since Event CRUD does not exist yet, no event can
currently reference a calendar, so this is unreachable today. Once Phase 5
adds Event CRUD, deleting a calendar that still has events will fail with a
foreign-key violation until that phase decides how to handle it (e.g.
blocking the delete, cascading, or reassigning events). No such policy has
been implemented yet — this is intentionally deferred, not solved, in Phase
4A.

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

Every endpoint other than `POST /api/auth/register`, `POST /api/auth/login`,
and the Swagger/OpenAPI paths now requires a valid Bearer token.

---

# Next Phase

## Phase 5 — Event CRUD

Goals:

- Event DTOs
- `EventService`
- Event CRUD endpoints, scoped to the authenticated user's calendars
- Decide how calendar deletion interacts with existing events (see the
  known limitation noted under Phase 4A)
- Event tests

---

# Remaining Planned Phases

- **Phase 5** — Event CRUD.
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
