package com.ballastlane.pokedex.domain.shared;

/** Base of every business-rule violation; adapters translate subclasses to their protocol. */
public abstract class DomainException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  protected DomainException(String message) {
    super(message);
  }
}
