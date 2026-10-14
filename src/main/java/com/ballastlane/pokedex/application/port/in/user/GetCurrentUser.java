package com.ballastlane.pokedex.application.port.in.user;

import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;

/** Use case: load the account behind an authenticated request. */
public interface GetCurrentUser {

  /**
   * @throws com.ballastlane.pokedex.domain.user.UserNotFoundException the account no longer exists
   */
  User byId(UserId id);
}
