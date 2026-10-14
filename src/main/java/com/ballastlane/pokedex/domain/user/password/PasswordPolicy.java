package com.ballastlane.pokedex.domain.user.password;

import java.util.List;
import java.util.Optional;

/**
 * Password strength policy as a Chain of Responsibility: each {@link PasswordRule} inspects the
 * password and may report a violation. Unlike a classic chain it does not stop at the first
 * failure, so the caller learns every broken rule in one round trip. New rules are added as new
 * links without touching the existing ones.
 */
public final class PasswordPolicy {

  public static final int MIN_LENGTH = 8;

  /** BCrypt only reads the first 72 bytes of a password. */
  public static final int MAX_LENGTH = 72;

  private final List<PasswordRule> rules;

  public PasswordPolicy(List<PasswordRule> rules) {
    this.rules = List.copyOf(rules);
  }

  /** The policy used for registration. */
  public static PasswordPolicy standard() {
    return new PasswordPolicy(
        List.of(
            new LengthRule(MIN_LENGTH, MAX_LENGTH),
            CharacterClassRule.upperCase(),
            CharacterClassRule.lowerCase(),
            CharacterClassRule.digit(),
            CharacterClassRule.special()));
  }

  /** Every broken rule, in chain order; empty when the password is acceptable. */
  public List<String> violationsOf(String password) {
    if (password == null) {
      return List.of("is required");
    }
    return rules.stream().map(rule -> rule.violation(password)).flatMap(Optional::stream).toList();
  }

  /** Throws {@link WeakPasswordException} listing every broken rule. */
  public void check(String password) {
    List<String> violations = violationsOf(password);
    if (!violations.isEmpty()) {
      throw new WeakPasswordException(violations);
    }
  }
}
