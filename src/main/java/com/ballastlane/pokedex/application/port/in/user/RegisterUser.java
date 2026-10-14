package com.ballastlane.pokedex.application.port.in.user;

import com.ballastlane.pokedex.domain.user.User;

/** Use case: create a regular account. */
public interface RegisterUser {

  /**
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldException invalid username or email
   * @throws com.ballastlane.pokedex.domain.user.password.WeakPasswordException weak password
   * @throws com.ballastlane.pokedex.domain.user.DuplicateUserException username or email taken
   */
  User register(RegisterUserCommand command);
}
