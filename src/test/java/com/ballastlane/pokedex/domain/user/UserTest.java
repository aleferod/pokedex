package com.ballastlane.pokedex.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

  private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
  private static final PasswordHash HASH =
      new PasswordHash("$2a$10$abcdefghijklmnopqrstuuJ0Zr1y9rRZ6o8QvYV3uN5x3Zp7KQ2y");

  @Test
  void should_createRegularUserWithEqualTimestamps_when_registered() {
    // Given
    UserId id = UserId.random();

    // When
    User user =
        User.register(id, new Username("ash"), new EmailAddress("ash@pallet.town"), HASH, NOW);

    // Then
    assertThat(user.id()).isEqualTo(id);
    assertThat(user.role()).isEqualTo(Role.USER);
    assertThat(user.createdAt()).isEqualTo(NOW);
    assertThat(user.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void should_keepTheChosenRole_when_provisionedByAnOperator() {
    User admin =
        User.provision(
            UserId.random(),
            new Username("admin"),
            new EmailAddress("admin@pokedex.example"),
            HASH,
            Role.ADMIN,
            NOW);

    assertThat(admin.role()).isEqualTo(Role.ADMIN);
    assertThat(admin.createdAt()).isEqualTo(NOW);
    assertThat(admin.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void should_keepEveryField_when_restoredFromStorage() {
    // When
    User user =
        User.restore(
            UserId.random(),
            new Username("oak"),
            new EmailAddress("oak@pallet.town"),
            HASH,
            Role.ADMIN,
            NOW,
            NOW.plusSeconds(60));

    // Then
    assertThat(user.role()).isEqualTo(Role.ADMIN);
    assertThat(user.updatedAt()).isEqualTo(NOW.plusSeconds(60));
  }

  @Test
  void should_reject_when_aRequiredPartIsMissing() {
    assertThatThrownBy(
            () -> User.register(null, new Username("ash"), new EmailAddress("a@b.io"), HASH, NOW))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @Test
  void should_notRevealThePasswordHash_when_printed() {
    User user =
        User.register(
            UserId.random(), new Username("ash"), new EmailAddress("ash@pallet.town"), HASH, NOW);

    assertThat(user.toString()).doesNotContain(HASH.value());
  }
}
