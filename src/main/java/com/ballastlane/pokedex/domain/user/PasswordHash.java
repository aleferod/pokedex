package com.ballastlane.pokedex.domain.user;

/**
 * Value object: the one-way hash of a password, opaque to the domain. Its {@code toString} is
 * redacted so the hash can never end up in logs or error messages.
 */
public record PasswordHash(String value) {

  public PasswordHash {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("password hash is required");
    }
  }

  @Override
  public String toString() {
    return "PasswordHash[redacted]";
  }
}
