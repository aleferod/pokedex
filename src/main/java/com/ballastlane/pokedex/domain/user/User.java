package com.ballastlane.pokedex.domain.user;

import java.time.Instant;
import java.util.Objects;

/**
 * A registered account. Created only through the factory methods: {@link #register} for
 * self-registered accounts (always {@link Role#USER}), {@link #provision} for accounts an operator
 * creates with a chosen role, and {@link #restore} for accounts loaded from storage.
 */
public record User(
    UserId id,
    Username username,
    EmailAddress email,
    PasswordHash passwordHash,
    Role role,
    Instant createdAt,
    Instant updatedAt) {

  public User {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(passwordHash, "passwordHash");
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
  }

  /** A new self-registered account: role is never chosen by the caller. */
  public static User register(
      UserId id, Username username, EmailAddress email, PasswordHash passwordHash, Instant now) {
    return new User(id, username, email, passwordHash, Role.USER, now, now);
  }

  /** A new account an operator creates with a chosen role (never through public registration). */
  public static User provision(
      UserId id,
      Username username,
      EmailAddress email,
      PasswordHash passwordHash,
      Role role,
      Instant now) {
    return new User(id, username, email, passwordHash, role, now, now);
  }

  /** An account as it was stored. */
  public static User restore(
      UserId id,
      Username username,
      EmailAddress email,
      PasswordHash passwordHash,
      Role role,
      Instant createdAt,
      Instant updatedAt) {
    return new User(id, username, email, passwordHash, role, createdAt, updatedAt);
  }
}
