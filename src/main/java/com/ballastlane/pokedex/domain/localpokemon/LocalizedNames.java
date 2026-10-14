package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Value object: the Pokémon's name per locale (proprietary data), e.g. {@code pt-BR -> Pikachu}.
 * Locales are BCP-47-like tags; names are trimmed, 1 to 50 characters; at most 10 locales. Kept
 * sorted by locale so equal content is always equal.
 */
public record LocalizedNames(Map<String, String> names) {

  public static final int MAX_LOCALES = 10;
  public static final int MAX_NAME_LENGTH = 50;
  private static final Pattern LOCALE = Pattern.compile("[a-z]{2,3}(-[A-Za-z0-9]{2,8}){0,2}");

  public LocalizedNames {
    TreeMap<String, String> validated = new TreeMap<>();
    if (names != null) {
      if (names.size() > MAX_LOCALES) {
        throw new InvalidFieldException(
            "localizedNames", "must have at most " + MAX_LOCALES + " entries");
      }
      names.forEach((locale, name) -> validated.put(locale(locale), name(locale, name)));
    }
    names = Collections.unmodifiableMap(validated);
  }

  public static LocalizedNames empty() {
    return new LocalizedNames(Map.of());
  }

  public static LocalizedNames of(Map<String, String> names) {
    return new LocalizedNames(names);
  }

  private static String locale(String locale) {
    if (locale == null || locale.length() > 35 || !LOCALE.matcher(locale).matches()) {
      throw new InvalidFieldException(
          "localizedNames", "'" + locale + "' is not a locale such as en, pt-BR or zh-Hant-TW");
    }
    return locale;
  }

  private static String name(String locale, String name) {
    String trimmed = name == null ? "" : name.trim();
    int length = trimmed.codePointCount(0, trimmed.length());
    if (length < 1 || length > MAX_NAME_LENGTH) {
      throw new InvalidFieldException(
          "localizedNames." + locale, "must be 1 to " + MAX_NAME_LENGTH + " characters");
    }
    return trimmed;
  }
}
