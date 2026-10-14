package com.ballastlane.pokedex.domain.user;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value object: a login name, trimmed and lower-cased so uniqueness is case-insensitive. 3 to 30
 * ASCII letters, digits, dots, underscores or hyphens.
 */
public record Username(String value) {

  private static final Pattern ALLOWED = Pattern.compile("[a-z0-9._-]{3,30}");

  public Username {
    if (value == null || value.isBlank()) {
      throw new InvalidFieldException("username", "is required");
    }
    value = value.trim().toLowerCase(Locale.ROOT);
    if (!ALLOWED.matcher(value).matches()) {
      throw new InvalidFieldException(
          "username", "must be 3 to 30 characters: letters, digits, '.', '_' or '-'");
    }
  }
}
