package com.ballastlane.pokedex.domain.user.password;

import java.util.Optional;
import java.util.function.IntPredicate;

/** Requires at least one character of a given class (upper case, lower case, digit, special). */
public final class CharacterClassRule implements PasswordRule {

  private final IntPredicate characterClass;
  private final String violation;

  private CharacterClassRule(IntPredicate characterClass, String violation) {
    this.characterClass = characterClass;
    this.violation = violation;
  }

  public static CharacterClassRule upperCase() {
    return new CharacterClassRule(Character::isUpperCase, "must contain an upper-case letter");
  }

  public static CharacterClassRule lowerCase() {
    return new CharacterClassRule(Character::isLowerCase, "must contain a lower-case letter");
  }

  public static CharacterClassRule digit() {
    return new CharacterClassRule(Character::isDigit, "must contain a digit");
  }

  public static CharacterClassRule special() {
    return new CharacterClassRule(
        codePoint -> !Character.isLetterOrDigit(codePoint),
        "must contain a character that is neither a letter nor a digit");
  }

  @Override
  public Optional<String> violation(String password) {
    return password.codePoints().anyMatch(characterClass)
        ? Optional.empty()
        : Optional.of(violation);
  }
}
