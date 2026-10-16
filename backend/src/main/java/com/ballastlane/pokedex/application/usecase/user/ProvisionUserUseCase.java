package com.ballastlane.pokedex.application.usecase.user;

import com.ballastlane.pokedex.application.port.in.user.ProvisionUser;
import com.ballastlane.pokedex.application.port.in.user.ProvisionUserCommand;
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
 * Creates an account with a chosen role, for operators and the demo-data seeder. Same rules as
 * registration (value objects, password policy, unique email); an existing username is left
 * untouched, which makes provisioning safe to repeat.
 */
public class ProvisionUserUseCase implements ProvisionUser {

  private final UserRepositoryPort users;
  private final PasswordHasherPort hasher;
  private final PasswordPolicy policy;
  private final Clock clock;

  public ProvisionUserUseCase(
      UserRepositoryPort users, PasswordHasherPort hasher, PasswordPolicy policy, Clock clock) {
    this.users = users;
    this.hasher = hasher;
    this.policy = policy;
    this.clock = clock;
  }

  @Override
  public boolean provision(ProvisionUserCommand command) {
    Username username = new Username(command.username());
    EmailAddress email = new EmailAddress(command.email());
    policy.check(command.password());
    if (users.existsByUsername(username)) {
      return false;
    }
    if (users.existsByEmail(email)) {
      throw new DuplicateUserException("email");
    }
    users.save(
        User.provision(
            UserId.random(),
            username,
            email,
            hasher.hash(command.password()),
            command.role(),
            clock.instant()));
    return true;
  }
}
