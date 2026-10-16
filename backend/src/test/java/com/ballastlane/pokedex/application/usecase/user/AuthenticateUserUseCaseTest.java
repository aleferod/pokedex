package com.ballastlane.pokedex.application.usecase.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.in.user.AuthenticatedUser;
import com.ballastlane.pokedex.application.port.in.user.Credentials;
import com.ballastlane.pokedex.application.port.out.user.IssuedToken;
import com.ballastlane.pokedex.application.port.out.user.PasswordHasherPort;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.InvalidCredentialsException;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.Username;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

  private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
  private static final PasswordHash STORED = new PasswordHash("$2a$04$stored");
  private static final PasswordHash DUMMY = new PasswordHash("$2a$04$dummy");
  private static final User ASH =
      User.register(
          UserId.random(), new Username("ash"), new EmailAddress("ash@pallet.town"), STORED, NOW);

  @Mock private UserRepositoryPort users;
  @Mock private PasswordHasherPort hasher;
  @Mock private TokenPort tokens;

  private AuthenticateUserUseCase authenticateUser;

  @BeforeEach
  void setUp() {
    given(hasher.hash(anyString())).willReturn(DUMMY);
    authenticateUser = new AuthenticateUserUseCase(users, hasher, tokens);
  }

  @Test
  void should_issueToken_when_credentialsAreValid() {
    // Given
    IssuedToken token = new IssuedToken("signed.jwt.value", NOW.plusSeconds(3600));
    given(users.findByUsername(new Username("ash"))).willReturn(Optional.of(ASH));
    given(hasher.matches("Pikachu@25", STORED)).willReturn(true);
    given(tokens.issue(ASH)).willReturn(token);

    // When
    AuthenticatedUser result = authenticateUser.authenticate(new Credentials("Ash", "Pikachu@25"));

    // Then
    assertThat(result.user()).isEqualTo(ASH);
    assertThat(result.token()).isEqualTo(token);
  }

  @Test
  void should_rejectWithoutToken_when_passwordIsWrong() {
    // Given
    given(users.findByUsername(new Username("ash"))).willReturn(Optional.of(ASH));
    given(hasher.matches("Wrong@1234", STORED)).willReturn(false);

    // When / Then
    assertThatThrownBy(() -> authenticateUser.authenticate(new Credentials("ash", "Wrong@1234")))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid username or password.");
    then(tokens).should(never()).issue(any());
  }

  @Test
  void should_rejectAfterCheckingDummyHash_when_usernameIsUnknown() {
    // Given
    given(users.findByUsername(new Username("nobody"))).willReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> authenticateUser.authenticate(new Credentials("nobody", "Pikachu@25")))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid username or password.");
    // The same hashing work is done as for a real account, so timing does not reveal accounts.
    then(hasher).should().matches("Pikachu@25", DUMMY);
    then(tokens).shouldHaveNoInteractions();
  }

  @Test
  void should_rejectAsInvalidCredentials_when_usernameIsNotEvenWellFormed() {
    assertThatThrownBy(() -> authenticateUser.authenticate(new Credentials("a!", "Pikachu@25")))
        .isInstanceOf(InvalidCredentialsException.class);
    then(users).shouldHaveNoInteractions();
    then(hasher).should().matches("Pikachu@25", DUMMY);
  }

  @Test
  void should_rejectAsInvalidCredentials_when_passwordIsMissing() {
    // Given
    given(users.findByUsername(new Username("ash"))).willReturn(Optional.of(ASH));

    // When / Then
    assertThatThrownBy(() -> authenticateUser.authenticate(new Credentials("ash", null)))
        .isInstanceOf(InvalidCredentialsException.class);
    then(tokens).shouldHaveNoInteractions();
  }

  @Test
  void should_neverPrintThePassword_when_credentialsAreLogged() {
    assertThat(new Credentials("ash", "Pikachu@25").toString()).doesNotContain("Pikachu@25");
    assertThat(new IssuedToken("signed.jwt.value", NOW).toString()).doesNotContain("signed.jwt");
  }
}
