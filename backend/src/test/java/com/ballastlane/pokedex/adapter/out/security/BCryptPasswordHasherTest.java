package com.ballastlane.pokedex.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.domain.user.PasswordHash;
import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

  private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(4);

  @Test
  void should_produceSaltedBcryptHash_when_hashing() {
    // When
    PasswordHash first = hasher.hash("Pikachu@25");
    PasswordHash second = hasher.hash("Pikachu@25");

    // Then
    assertThat(first.value()).isNotEqualTo("Pikachu@25").startsWith("$2").contains("$04$");
    assertThat(second).isNotEqualTo(first);
  }

  @Test
  void should_matchOnlyTheOriginalPassword_when_verifying() {
    // Given
    PasswordHash hash = hasher.hash("Pikachu@25");

    // When / Then
    assertThat(hasher.matches("Pikachu@25", hash)).isTrue();
    assertThat(hasher.matches("pikachu@25", hash)).isFalse();
    assertThat(hasher.matches("", hash)).isFalse();
  }
}
