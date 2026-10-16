package com.ballastlane.pokedex.application.port.in.user;

/**
 * Use case: an operator (or the demo-data seeder) creates an account with a chosen role. Not
 * exposed over HTTP; public registration always creates {@code USER} accounts.
 */
public interface ProvisionUser {

  /**
   * Creates the account unless its username is taken.
   *
   * @return {@code true} if the account was created, {@code false} if the username already exists
   *     (the existing account is left untouched)
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldException malformed username or email
   * @throws com.ballastlane.pokedex.domain.user.password.WeakPasswordException the password breaks
   *     the policy
   * @throws com.ballastlane.pokedex.domain.user.DuplicateUserException another account has the
   *     email
   */
  boolean provision(ProvisionUserCommand command);
}
