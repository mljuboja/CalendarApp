-- Phase 2: initial schema for the approved Phase 1 domain model.
-- Hibernate is configured to VALIDATE against this schema, never generate it.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE calendars (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    color   VARCHAR(7) NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    user_id BIGINT NOT NULL REFERENCES users (id)
);

CREATE INDEX idx_calendars_user_id ON calendars (user_id);

CREATE TABLE categories (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    color   VARCHAR(7) NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    user_id BIGINT NOT NULL REFERENCES users (id)
);

CREATE INDEX idx_categories_user_id ON categories (user_id);

CREATE TABLE events (
    id                      BIGSERIAL PRIMARY KEY,
    title                   VARCHAR(200) NOT NULL,
    description             TEXT,
    location                VARCHAR(255),
    start_time              TIMESTAMP NOT NULL,
    end_time                TIMESTAMP NOT NULL,
    all_day                 BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_type         VARCHAR(20) NOT NULL DEFAULT 'NONE'
                                CHECK (recurrence_type IN ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY')),
    reminder_offset_minutes INTEGER,
    calendar_id             BIGINT NOT NULL REFERENCES calendars (id),
    category_id             BIGINT REFERENCES categories (id),
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_calendar_id ON events (calendar_id);
CREATE INDEX idx_events_category_id ON events (category_id);

CREATE TABLE tasks (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    due_date    DATE,
    priority    VARCHAR(10) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED')),
    user_id     BIGINT NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_user_id ON tasks (user_id);
