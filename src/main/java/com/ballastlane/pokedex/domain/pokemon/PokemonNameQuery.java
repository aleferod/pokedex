package com.ballastlane.pokedex.domain.pokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Value object: part of a Pokémon's name to search the catalog by. It is normalised like PokeAPI's
 * names (lower case, a hyphen for each run of spaces) and long enough to mean something: one or two
 * letters would match most of the catalog.
 */
public record PokemonNameQuery(String value) {

  public static final String FIELD = "name";
  public static final String RULE = "must be 3 to 50 letters, digits, hyphens or spaces";

  private static final Pattern VALID = Pattern.compile("[a-z0-9-]{3,50}");
  private static final Pattern SPACES = Pattern.compile("\\s+");

  public PokemonNameQuery {
    if (value == null || !VALID.matcher(value).matches()) {
      throw new InvalidFieldException(FIELD, RULE);
    }
  }

  /**
   * Parses what a client typed: {@code Pika}, {@code mr mime}. Empty for a missing or blank name,
   * which means no filter.
   *
   * @throws InvalidFieldException the name breaks the rule above
   */
  public static Optional<PokemonNameQuery> parse(String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    String normalised = SPACES.matcher(raw.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
    return Optional.of(new PokemonNameQuery(normalised));
  }

  /** Whether a (lower-case) PokeAPI name contains this query. */
  public boolean matches(String name) {
    return name.contains(value);
  }
}
