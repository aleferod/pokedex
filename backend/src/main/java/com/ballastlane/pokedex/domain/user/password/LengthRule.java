package com.ballastlane.pokedex.domain.user.password;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Bounds the length in characters and, for BCrypt's sake, the UTF-8 size in bytes. */
public final class LengthRule implements PasswordRule {

  private final int min;
  private final int max;

  public LengthRule(int min, int max) {
    this.min = min;
    this.max = max;
  }

  @Override
  public Optional<String> violation(String password) {
    int characters = password.codePointCount(0, password.length());
    int bytes = password.getBytes(StandardCharsets.UTF_8).length;
    if (characters < min || characters > max || bytes > max) {
      return Optional.of("must be " + min + " to " + max + " characters");
    }
    return Optional.empty();
  }
}
