package com.ballastlane.pokedex.application.usecase.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.UserNotFoundException;
import com.ballastlane.pokedex.domain.user.Username;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseTest {

  @Mock private UserRepositoryPort users;
  @InjectMocks private GetCurrentUserUseCase getCurrentUser;

  @Test
  void should_returnAccount_when_itExists() {
    // Given
    User ash =
        User.register(
            UserId.random(),
            new Username("ash"),
            new EmailAddress("ash@pallet.town"),
            new PasswordHash("$2a$04$hash"),
            Instant.parse("2026-10-14T12:00:00Z"));
    given(users.findById(ash.id())).willReturn(Optional.of(ash));

    // When / Then
    assertThat(getCurrentUser.byId(ash.id())).isEqualTo(ash);
  }

  @Test
  void should_throwNotFound_when_accountNoLongerExists() {
    // Given
    UserId gone = UserId.random();
    given(users.findById(gone)).willReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> getCurrentUser.byId(gone)).isInstanceOf(UserNotFoundException.class);
  }
}
