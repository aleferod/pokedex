package com.ballastlane.pokedex.domain.user;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value object: an email address, trimmed and lower-cased so uniqueness is case-insensitive.
 * Deliberately pragmatic syntax check ({@code local@domain.tld}, at most 254 characters); proving
 * ownership would need a verification mail, which is out of scope.
 */
public record EmailAddress(String value) {

  public static final int MAX_LENGTH = 254;

  private static final Pattern SYNTAX =
      Pattern.compile("[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)+");

  public EmailAddress {
    if (value == null || value.isBlank()) {
      throw new InvalidFieldException("email", "is required");
    }
    value = value.trim().toLowerCase(Locale.ROOT);
    if (value.length() > MAX_LENGTH || !SYNTAX.matcher(value).matches()) {
      throw new InvalidFieldException(
          "email", "must be a valid email address of at most " + MAX_LENGTH + " characters");
    }
  }
}
