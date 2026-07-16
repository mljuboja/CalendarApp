# Calendar App — Design Plan (Approved)

This document captures the finalized Phase 1 decisions before moving into implementation.

## Tech stack
- Java 21, Spring Boot (Maven)
- Spring Web, Spring Data JPA, Spring Security (for password hashing only, via `BCryptPasswordEncoder`)
- Bean Validation (`spring-boot-starter-validation`)
- H2 for local/dev persistence (easy to swap for Postgres later)
- Layered architecture: `controller` → `service` → `repository` → `entity`, plus `dto` and `mapper` packages

## Entities

### User
- `id: Long`
- `username: String`
- `email: String`
- `passwordHash: String` — never expose the raw password; always store a hash
- No back-reference to Calendars (kept unidirectional)

### Calendar
- `id: Long`
- `name: String`
- `userId: Long` (or a unidirectional `@ManyToOne User owner`) — owns the calendar, no `List<Event>` back-reference

### Event
- `id: Long`
- `title: String`
- `description: String`
- `startTime: LocalDateTime`
- `endTime: LocalDateTime`
- `recurrenceRule: String` — simple rule description (e.g. frequency + interval), used to generate occurrences on demand
- `calendar: Calendar` — unidirectional `@ManyToOne`, no `List<Event>` on `Calendar`
- `createdAt: LocalDateTime` — set once on creation
- `updatedAt: LocalDateTime` — refreshed on every update

**No `Instant` anywhere.** `LocalDateTime` is used consistently for `startTime`, `endTime`, `createdAt`, and `updatedAt`. This app has no multi-timezone requirement, so `LocalDateTime` is simpler to reason about and explain.

## Relationships
- Unidirectional only: `Event → Calendar` and `Calendar → User`.
- No collection-valued back-references (`Calendar.events`, `User.calendars`) — avoids common beginner Hibernate lazy-loading / serialization pitfalls.

## IDs
- `Long` (auto-increment) for all entity primary keys. Simpler than UUID, matches most tutorials, easy to explain.

## Recurrence strategy
- Recurring events are **not** pre-generated into the database.
- Occurrences are computed on demand, only for the requested date range (e.g. when fetching events for a given week/month).
- Keeps the events table small and avoids managing thousands of duplicate rows.

## API design highlights
- Standard REST resources: `/api/users`, `/api/calendars`, `/api/events`.
- Password changes use a dedicated endpoint: `PUT /api/users/me/password` (separate from general profile updates at `/api/users/me`).

## Auditing
- `Event.createdAt` / `Event.updatedAt` populated automatically (e.g. via `@PrePersist` / `@PreUpdate` or Spring Data auditing).

## Working agreement on explanations
- Explain a new concept (annotation, pattern, library) the first time it's introduced.
- Do not repeat the same explanation on subsequent occurrences unless explicitly asked again.

## Phase 2 scope
- Scaffold the Maven/Spring Boot project structure.
- Create package layout: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `config`, `exception`.
- Implement `User`, `Calendar`, and `Event` entities per the design above.
- Wire up a working local build (H2 in-memory DB) with no business logic yet — that comes in later phases (repositories, DTOs/mappers, services, controllers, security).
