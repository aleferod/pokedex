package com.ballastlane.pokedex.domain.user;

import com.ballastlane.pokedex.domain.shared.DomainException;

/** No account has the given id (for example, it was deleted after a token was issued). */
public class UserNotFoundException extends DomainException {

  private static final long serialVersionUID = 1L;

  public UserNotFoundException(UserId id) {
    super("No account with id " + id.value());
  }
}
