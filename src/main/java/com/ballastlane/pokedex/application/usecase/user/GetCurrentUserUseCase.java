package com.ballastlane.pokedex.application.usecase.user;

import com.ballastlane.pokedex.application.port.in.user.GetCurrentUser;
import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.UserNotFoundException;

/** Loads the account an access token refers to. */
public class GetCurrentUserUseCase implements GetCurrentUser {

  private final UserRepositoryPort users;

  public GetCurrentUserUseCase(UserRepositoryPort users) {
    this.users = users;
  }

  @Override
  public User byId(UserId id) {
    return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}
