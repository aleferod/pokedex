package com.ballastlane.pokedex.application.usecase.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.in.user.RegisterUserCommand;
import com.ballastlane.pokedex.application.port.out.user.PasswordHasherPort;
import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.Username;
import com.ballastlane.pokedex.domain.user.password.PasswordPolicy;
import com.ballastlane.pokedex.domain.user.password.WeakPasswordException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

  private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
  private static final PasswordHash HASH = new PasswordHash("$2a$04$hashed");

  @Mock private UserRepositoryPort users;
  @Mock private PasswordHasherPort hasher;

  private RegisterUserUseCase registerUser;

  @BeforeEach
  void setUp() {
    registerUser =
        new RegisterUserUseCase(
            users, hasher, PasswordPolicy.standard(), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void should_saveHashedRegularUser_when_registrationIsValid() {
    // Given
    given(users.existsByUsername(new Username("ash"))).willReturn(false);
    given(users.existsByEmail(new EmailAddress("ash@pallet.town"))).willReturn(false);
    given(hasher.hash("Pikachu@25")).willReturn(HASH);
    given(users.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    // When
    User user =
        registerUser.register(new RegisterUserCommand("  Ash ", "Ash@Pallet.Town", "Pikachu@25"));

    // Then
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    then(users).should().save(saved.capture());
    assertThat(saved.getValue().username().value()).isEqualTo("ash");
    assertThat(saved.getValue().email().value()).isEqualTo("ash@pallet.town");
    assertThat(saved.getValue().passwordHash()).isEqualTo(HASH);
    assertThat(saved.getValue().role()).isEqualTo(Role.USER);
    assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
    assertThat(user).isEqualTo(saved.getValue());
  }

  @Test
  void should_rejectWithoutTouchingStorage_when_passwordIsWeak() {
    assertThatThrownBy(
            () ->
                registerUser.register(
                    new RegisterUserCommand("ash", "ash@pallet.town", "password")))
        .isInstanceOfSatisfying(
            WeakPasswordException.class, error -> assertThat(error.violations()).hasSize(3));
    then(users).shouldHaveNoInteractions();
    then(hasher).shouldHaveNoInteractions();
  }

  @Test
  void should_rejectNamingTheField_when_usernameIsInvalid() {
    assertThatThrownBy(
            () ->
                registerUser.register(
                    new RegisterUserCommand("a!", "ash@pallet.town", "Pikachu@25")))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("username"));
    then(users).shouldHaveNoInteractions();
  }

  @Test
  void should_rejectAsConflict_when_usernameIsTaken() {
    // Given
    given(users.existsByUsername(new Username("ash"))).willReturn(true);

    // When / Then
    assertThatThrownBy(
            () ->
                registerUser.register(
                    new RegisterUserCommand("ASH", "other@pallet.town", "Pikachu@25")))
        .isInstanceOfSatisfying(
            DuplicateUserException.class, error -> assertThat(error.field()).isEqualTo("username"));
    then(hasher).should(never()).hash(anyString());
    then(users).should(never()).save(any());
  }

  @Test
  void should_rejectAsConflict_when_emailIsTaken() {
    // Given
    given(users.existsByUsername(new Username("misty"))).willReturn(false);
    given(users.existsByEmail(new EmailAddress("ash@pallet.town"))).willReturn(true);

    // When / Then
    assertThatThrownBy(
            () ->
                registerUser.register(
                    new RegisterUserCommand("misty", "Ash@Pallet.Town", "Pikachu@25")))
        .isInstanceOfSatisfying(
            DuplicateUserException.class, error -> assertThat(error.field()).isEqualTo("email"));
    then(users).should(never()).save(any());
  }

  @Test
  void should_neverPrintThePassword_when_commandIsLogged() {
    assertThat(new RegisterUserCommand("ash", "ash@pallet.town", "Pikachu@25").toString())
        .doesNotContain("Pikachu@25");
  }
}
