package com.ballastlane.pokedex.infrastructure.usecase;

import com.ballastlane.pokedex.application.port.in.user.AuthenticateUser;
import com.ballastlane.pokedex.application.port.in.user.GetCurrentUser;
import com.ballastlane.pokedex.application.port.in.user.ProvisionUser;
import com.ballastlane.pokedex.application.port.in.user.RegisterUser;
import com.ballastlane.pokedex.application.port.out.user.PasswordHasherPort;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
import com.ballastlane.pokedex.application.usecase.user.AuthenticateUserUseCase;
import com.ballastlane.pokedex.application.usecase.user.GetCurrentUserUseCase;
import com.ballastlane.pokedex.application.usecase.user.ProvisionUserUseCase;
import com.ballastlane.pokedex.application.usecase.user.RegisterUserUseCase;
import com.ballastlane.pokedex.domain.user.password.PasswordPolicy;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free account use cases to their ports. The application layer has no Spring
 * annotations, so this is where the use cases become beans.
 */
@Configuration(proxyBeanMethods = false)
public class UserUseCaseConfiguration {

  @Bean
  RegisterUser registerUser(UserRepositoryPort users, PasswordHasherPort hasher, Clock clock) {
    return new RegisterUserUseCase(users, hasher, PasswordPolicy.standard(), clock);
  }

  @Bean
  ProvisionUser provisionUser(UserRepositoryPort users, PasswordHasherPort hasher, Clock clock) {
    return new ProvisionUserUseCase(users, hasher, PasswordPolicy.standard(), clock);
  }

  @Bean
  AuthenticateUser authenticateUser(
      UserRepositoryPort users, PasswordHasherPort hasher, TokenPort tokens) {
    return new AuthenticateUserUseCase(users, hasher, tokens);
  }

  @Bean
  GetCurrentUser getCurrentUser(UserRepositoryPort users) {
    return new GetCurrentUserUseCase(users);
  }
}
