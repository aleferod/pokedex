-- Accounts for the auxiliary auth API (add-user-auth).
-- Usernames and emails are lower-cased by the domain, so plain unique constraints give
-- case-insensitive uniqueness; the check constraints keep that invariant at the database level.
CREATE TABLE users (
    id            UUID         NOT NULL,
    username      VARCHAR(30)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_username_lower_case CHECK (username = lower(username)),
    CONSTRAINT ck_users_email_lower_case CHECK (email = lower(email))
);

COMMENT ON TABLE users IS 'Registered accounts; password_hash holds a BCrypt hash, never a password.';
