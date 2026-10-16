package com.ballastlane.pokedex.application.port.in.user;

/** Use case: exchange credentials for an access token. */
public interface AuthenticateUser {

  /**
   * @throws com.ballastlane.pokedex.domain.user.InvalidCredentialsException unknown username or
   *     wrong password (indistinguishable on purpose)
   */
  AuthenticatedUser authenticate(Credentials credentials);
}
