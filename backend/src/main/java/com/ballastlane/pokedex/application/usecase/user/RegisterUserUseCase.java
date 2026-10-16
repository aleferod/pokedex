package com.ballastlane.pokedex.application.usecase.user;

import com.ballastlane.pokedex.application.port.in.user.RegisterUser;
import com.ballastlane.pokedex.application.port.in.user.RegisterUserCommand;
import com.ballastlane.pokedex.application.port.out.user.PasswordHasherPort;
import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.Username;
import com.ballastlane.pokedex.domain.user.password.PasswordPolicy;
import java.time.Clock;

/**
 * Registers a regular account: validates through the domain, refuses duplicates, hashes the
 * password and stores the account. Cheap checks run first so nothing is hashed for a request that
 * will be rejected anyway.
 */
public class RegisterUserUseCase implements RegisterUser {

  private final UserRepositoryPort users;
  private final PasswordHasherPort hasher;
  private final PasswordPolicy policy;
  private final Clock clock;

  public RegisterUserUseCase(
      UserRepositoryPort users, PasswordHasherPort hasher, PasswordPolicy policy, Clock clock) {
    this.users = users;
    this.hasher = hasher;
    this.policy = policy;
    this.clock = clock;
  }

  @Override
  public User register(RegisterUserCommand command) {
    Username username = new Username(command.username());
    EmailAddress email = new EmailAddress(command.email());
    policy.check(command.password());
    if (users.existsByUsername(username)) {
      throw new DuplicateUserException("username");
    }
    if (users.existsByEmail(email)) {
      throw new DuplicateUserException("email");
    }
    User user =
        User.register(
            UserId.random(), username, email, hasher.hash(command.password()), clock.instant());
    return users.save(user);
  }
}
