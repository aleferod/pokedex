package com.ballastlane.pokedex.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.Username;
import com.ballastlane.pokedex.support.PostgresTestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
  PostgresTestcontainersConfiguration.class,
  UserPersistenceAdapter.class,
  UserPersistenceMapperImpl.class
})
class UserPersistenceAdapterIT {

  private static final Instant CREATED = Instant.parse("2026-10-14T12:00:00.123456Z");
  private static final PasswordHash HASH =
      new PasswordHash("$2a$04$Zr1y9rRZ6o8QvYV3uN5x3OeJ0abcdefghijklmnopqrstuvwxyz12");

  @Autowired private UserPersistenceAdapter adapter;
  @Autowired private EntityManager entityManager;

  @Test
  void should_findSavedAccount_when_lookedUpByIdOrUsername() {
    // Given
    User ash = user("ash", "ash@pallet.town");

    // When
    User saved = adapter.save(ash);

    // Then
    assertThat(saved).isEqualTo(ash);
    assertThat(adapter.findById(ash.id())).contains(ash);
    assertThat(adapter.findByUsername(new Username("ash"))).contains(ash);
    assertThat(adapter.findByUsername(new Username("misty"))).isEmpty();
    assertThat(adapter.findById(UserId.random())).isEmpty();
  }

  @Test
  void should_reportExistence_when_usernameOrEmailIsUsed() {
    // Given
    adapter.save(user("ash", "ash@pallet.town"));

    // When / Then
    assertThat(adapter.existsByUsername(new Username("ASH"))).isTrue();
    assertThat(adapter.existsByEmail(new EmailAddress("Ash@Pallet.Town"))).isTrue();
    assertThat(adapter.existsByUsername(new Username("misty"))).isFalse();
    assertThat(adapter.existsByEmail(new EmailAddress("misty@cerulean.city"))).isFalse();
  }

  @Test
  void should_storeNormalizedColumns_when_accountIsSaved() {
    // When
    adapter.save(user("Brock", "Brock@Pewter.City"));

    // Then
    Object[] row =
        (Object[])
            entityManager
                .createNativeQuery(
                    "select username, email, role, password_hash from users where username = 'brock'")
                .getSingleResult();
    assertThat(row).containsExactly("brock", "brock@pewter.city", "USER", HASH.value());
  }

  @Test
  void should_throwDuplicateUsername_when_concurrentInsertTakesTheUsername() {
    // Given an account saved while this registration's existence check had already passed
    adapter.save(user("ash", "ash@pallet.town"));

    // When / Then
    assertThatThrownBy(() -> adapter.save(user("ash", "other@pallet.town")))
        .isInstanceOfSatisfying(
            DuplicateUserException.class, error -> assertThat(error.field()).isEqualTo("username"));
  }

  @Test
  void should_throwDuplicateEmail_when_concurrentInsertTakesTheEmail() {
    // Given
    adapter.save(user("ash", "ash@pallet.town"));

    // When / Then
    assertThatThrownBy(() -> adapter.save(user("misty", "ash@pallet.town")))
        .isInstanceOfSatisfying(
            DuplicateUserException.class, error -> assertThat(error.field()).isEqualTo("email"));
  }

  @Test
  void should_keepRoleAndTimestamps_when_restoredAccountIsSaved() {
    // Given
    User admin =
        User.restore(
            UserId.random(),
            new Username("oak"),
            new EmailAddress("oak@pallet.town"),
            HASH,
            Role.ADMIN,
            CREATED,
            CREATED.plusSeconds(90));

    // When
    adapter.save(admin);
    entityManager.clear();

    // Then
    assertThat(adapter.findById(admin.id())).contains(admin);
  }

  private static User user(String username, String email) {
    return User.register(
        UserId.random(), new Username(username), new EmailAddress(email), HASH, CREATED);
  }
}
