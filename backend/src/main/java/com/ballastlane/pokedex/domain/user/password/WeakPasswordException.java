package com.ballastlane.pokedex.domain.user.password;

import com.ballastlane.pokedex.domain.shared.DomainException;
import java.util.List;

/** The password breaks one or more rules of the {@link PasswordPolicy}; lists every broken rule. */
public class WeakPasswordException extends DomainException {

  private static final long serialVersionUID = 1L;

  // An array (not a List) keeps the exception serializable without -Xlint:serial warnings.
  private final String[] violations;

  public WeakPasswordException(List<String> violations) {
    super("password " + String.join(", ", violations));
    this.violations = violations.toArray(String[]::new);
  }

  public List<String> violations() {
    return List.of(violations);
  }
}
