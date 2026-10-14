package com.ballastlane.pokedex.adapter.in.web.auth;

import com.ballastlane.pokedex.application.port.in.user.AuthenticatedUser;
import com.ballastlane.pokedex.application.port.in.user.Credentials;
import com.ballastlane.pokedex.application.port.in.user.RegisterUserCommand;
import com.ballastlane.pokedex.domain.user.User;

/** DTO + Mapper: translates between the auth API's JSON shapes and the use-case ports. */
final class AuthWebMapper {

  static final String BEARER = "Bearer";

  private AuthWebMapper() {}

  static RegisterUserCommand toCommand(RegisterRequest request) {
    return new RegisterUserCommand(request.username(), request.email(), request.password());
  }

  static Credentials toCredentials(LoginRequest request) {
    return new Credentials(request.username(), request.password());
  }

  static UserResponse toResponse(User user) {
    return new UserResponse(
        user.id().value(),
        user.username().value(),
        user.email().value(),
        user.role().name(),
        user.createdAt());
  }

  static TokenResponse toResponse(AuthenticatedUser authenticated) {
    return new TokenResponse(
        authenticated.token().value(),
        BEARER,
        authenticated.token().expiresAt(),
        toResponse(authenticated.user()));
  }
}
