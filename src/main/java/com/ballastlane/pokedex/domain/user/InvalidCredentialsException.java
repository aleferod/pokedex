package com.ballastlane.pokedex.domain.user;

import com.ballastlane.pokedex.domain.shared.DomainException;

/**
 * Login failed. Deliberately says nothing about whether the username exists, so it cannot be used
 * to discover accounts.
 */
public class InvalidCredentialsException extends DomainException {

  private static final long serialVersionUID = 1L;

  public InvalidCredentialsException() {
    super("Invalid username or password.");
  }
}
