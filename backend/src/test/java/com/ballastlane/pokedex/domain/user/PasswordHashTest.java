package com.ballastlane.pokedex.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class PasswordHashTest {

  private static final String BCRYPT =
      "$2a$10$abcdefghijklmnopqrstuuJ0Zr1y9rRZ6o8QvYV3uN5x3Zp7KQ2y";

  @Test
  void should_neverRevealTheHash_when_printed() {
    PasswordHash hash = new PasswordHash(BCRYPT);
    assertThat(hash.toString()).doesNotContain(BCRYPT).contains("redacted");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void should_reject_when_hashIsMissing(String value) {
    assertThatThrownBy(() -> new PasswordHash(value)).isInstanceOf(IllegalArgumentException.class);
  }
}
