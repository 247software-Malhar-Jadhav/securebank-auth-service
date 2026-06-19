-- V1__init.sql
-- Initial schema for the `auth` service. Database-per-service (spec §1): this service
-- owns the `auth` schema exclusively; no other service touches these tables.
--
-- Design note: we keep authentication state (failed_attempts, locked_until) on the
-- user row itself. For a single identity service this is simplest and transactional;
-- a larger system might move lockout state to Redis. `version` powers JPA optimistic
-- locking so concurrent login attempts don't clobber each other's counters.

CREATE TABLE users (
    id                BIGSERIAL    PRIMARY KEY,
    username          VARCHAR(64)  NOT NULL UNIQUE,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(100) NOT NULL,        -- BCrypt hash, never the raw password
    role              VARCHAR(32)  NOT NULL,        -- ADMIN | CUSTOMER (stored as string)
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_attempts   INTEGER      NOT NULL DEFAULT 0,
    locked_until      TIMESTAMPTZ  NULL,            -- non-null + future => account locked
    preferred_locale  VARCHAR(8)   NOT NULL DEFAULT 'en', -- en | hi | mr
    version           BIGINT       NOT NULL DEFAULT 0,     -- JPA optimistic lock
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Lookups during login/registration are by username & email, so index them.
-- (UNIQUE constraints above already create indexes; explicit ones documented for clarity.)
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email    ON users (email);
