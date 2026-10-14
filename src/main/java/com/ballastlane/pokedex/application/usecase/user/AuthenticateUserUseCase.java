package com.ballastlane.pokedex.application.usecase.user;

import com.ballastlane.pokedex.application.port.in.user.AuthenticateUser;
import com.ballastlane.pokedex.application.port.in.user.AuthenticatedUser;
import com.ballastlane.pokedex.application.port.in.user.Credentials;
import com.ballastlane.pokedex.application.port.out.user.PasswordHasherPort;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.user.InvalidCredentialsException;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.Username;
import java.util.Optional;
import java.util.UUID;

/**
 * Exchanges credentials for an access token. Every failure is the same {@link
 * InvalidCredentialsException}, and an unknown username still costs one hash comparison (against a
 * dummy hash), so neither the response nor its timing reveals which usernames exist.
 */
public class AuthenticateUserUseCase implements AuthenticateUser {

  private final UserRepositoryPort users;
  private final PasswordHasherPort hasher;
  private final TokenPort tokens;
  private final PasswordHash dummyHash;

  public AuthenticateUserUseCase(
      UserRepositoryPort users, PasswordHasherPort hasher, TokenPort tokens) {
    this.users = users;
    this.hasher = hasher;
    this.tokens = tokens;
    this.dummyHash = hasher.hash(UUID.randomUUID().toString());
  }

  @Override
  public AuthenticatedUser authenticate(Credentials credentials) {
    String password = credentials.password() == null ? "" : credentials.password();
    Optional<User> account = findAccount(credentials.username());
    if (account.isEmpty()) {
      hasher.matches(password, dummyHash);
      throw new InvalidCredentialsException();
    }
    User user = account.get();
    if (password.isEmpty() || !hasher.matches(password, user.passwordHash())) {
      throw new InvalidCredentialsException();
    }
    return new AuthenticatedUser(user, tokens.issue(user));
  }

  private Optional<User> findAccount(String username) {
    try {
      return users.findByUsername(new Username(username));
    } catch (InvalidFieldException notAUsername) {
      return Optional.empty();
    }
  }
}
