package com.ballastlane.pokedex.domain.shared;

/** A single input value breaks a domain rule; names the field so clients can show it in place. */
public class InvalidFieldException extends DomainException {

  private static final long serialVersionUID = 1L;

  private final String field;
  private final String reason;

  public InvalidFieldException(String field, String reason) {
    super(field + " " + reason);
    this.field = field;
    this.reason = reason;
  }

  public String field() {
    return field;
  }

  public String reason() {
    return reason;
  }
}
