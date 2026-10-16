package com.ballastlane.pokedex.domain.localpokemon;

import java.util.Locale;

/**
 * Value object: which local records a listing wants. Both criteria are optional and combine. The
 * tag is normalized like {@link InternalTags} (trimmed, lower case) so it matches stored tags
 * exactly; the name is a case-insensitive substring. Blank criteria are ignored.
 */
public record LocalPokemonFilter(String tag, String name) {

  public LocalPokemonFilter {
    tag = normalize(tag);
    name = normalize(name);
  }

  public static LocalPokemonFilter none() {
    return new LocalPokemonFilter(null, null);
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
  }
}
