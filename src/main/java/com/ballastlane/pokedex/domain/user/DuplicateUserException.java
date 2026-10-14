package com.ballastlane.pokedex.domain.user;

import com.ballastlane.pokedex.domain.shared.DomainException;

/** Another account already uses this username or email address. */
public class DuplicateUserException extends DomainException {

  private static final long serialVersionUID = 1L;

  private final String field;

  public DuplicateUserException(String field) {
    super(field + " is already taken");
    this.field = field;
  }

  /** {@code username} or {@code email}. */
  public String field() {
    return field;
  }
}
