package com.ballastlane.pokedex.domain.shared;

import java.util.List;

/**
 * Several input values break domain rules at once (Notification pattern): callers get every problem
 * in one answer instead of fixing them one round trip at a time.
 */
public class InvalidFieldsException extends DomainException {

  private static final long serialVersionUID = 1L;

  // An array keeps the exception serializable without -Xlint:serial warnings.
  private final InvalidFieldException[] violations;

  public InvalidFieldsException(List<InvalidFieldException> violations) {
    super(violations.size() + " invalid field(s)");
    this.violations = violations.toArray(InvalidFieldException[]::new);
  }

  public List<InvalidFieldException> violations() {
    return List.of(violations);
  }
}
