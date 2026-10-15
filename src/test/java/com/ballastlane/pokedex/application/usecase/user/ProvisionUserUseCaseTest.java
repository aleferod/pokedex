package com.ballastlane.pokedex.application.usecase.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.in.user.ProvisionUserCommand;
import com.ballastlane.pokedex.application.port.out.user.PasswordHasherPort;
import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
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
class ProvisionUserUseCaseTest {

  private static final Instant NOW = Instant.parse("2026-10-15T09:00:00Z");
  private static final PasswordHash HASH = new PasswordHash("$2a$04$hashed");
  private static final ProvisionUserCommand ADMIN =
      new ProvisionUserCommand("admin", "admin@pokedex.example", "Admin@123", Role.ADMIN);

  @Mock private UserRepositoryPort users;
  @Mock private PasswordHasherPort hasher;

  private ProvisionUserUseCase provisionUser;

  @BeforeEach
  void setUp() {
    provisionUser =
        new ProvisionUserUseCase(
            users, hasher, PasswordPolicy.standard(), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void should_saveAHashedAccountWithTheChosenRole_when_theUsernameIsFree() {
    // Given
    given(users.existsByUsername(new Username("admin"))).willReturn(false);
    given(users.existsByEmail(new EmailAddress("admin@pokedex.example"))).willReturn(false);
    given(hasher.hash("Admin@123")).willReturn(HASH);
    given(users.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    // When
    boolean created = provisionUser.provision(ADMIN);

    // Then
    assertThat(created).isTrue();
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    then(users).should().save(saved.capture());
    assertThat(saved.getValue().username()).isEqualTo(new Username("admin"));
    assertThat(saved.getValue().role()).isEqualTo(Role.ADMIN);
    assertThat(saved.getValue().passwordHash()).isEqualTo(HASH);
    assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
  }

  @Test
  void should_leaveTheExistingAccountAlone_when_theUsernameIsTaken() {
    given(users.existsByUsername(new Username("admin"))).willReturn(true);

    assertThat(provisionUser.provision(ADMIN)).isFalse();
    then(hasher).should(never()).hash(anyString());
    then(users).should(never()).save(any());
  }

  @Test
  void should_rejectWithoutHashing_when_thePasswordIsWeak() {
    ProvisionUserCommand weak =
        new ProvisionUserCommand("admin", "admin@pokedex.example", "admin", Role.ADMIN);

    assertThatThrownBy(() -> provisionUser.provision(weak))
        .isInstanceOf(WeakPasswordException.class);
    then(hasher).should(never()).hash(anyString());
    then(users).should(never()).save(any());
  }

  @Test
  void should_refuse_when_anotherAccountHasTheEmail() {
    given(users.existsByUsername(new Username("admin"))).willReturn(false);
    given(users.existsByEmail(new EmailAddress("admin@pokedex.example"))).willReturn(true);

    assertThatThrownBy(() -> provisionUser.provision(ADMIN))
        .isInstanceOfSatisfying(
            DuplicateUserException.class, error -> assertThat(error.field()).isEqualTo("email"));
    then(users).should(never()).save(any());
  }

  @Test
  void should_notRevealThePassword_when_theCommandIsPrinted() {
    assertThat(ADMIN.toString()).doesNotContain("Admin@123").contains("admin", "ADMIN");
  }
}
