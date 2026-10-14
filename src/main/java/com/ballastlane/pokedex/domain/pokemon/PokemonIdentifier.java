package com.ballastlane.pokedex.domain.pokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value object: how a client names a Pokémon, either by Pokédex number or by (case-insensitive)
 * name. Malformed input is rejected here, before any upstream call is made.
 */
public sealed interface PokemonIdentifier permits PokemonIdentifier.ById, PokemonIdentifier.ByName {

  int MAX_NUMBER = 99_999;
  String FIELD = "idOrName";
  String RULE =
      "must be a Pokédex number from 1 to "
          + MAX_NUMBER
          + " or a name of up to 50 letters, digits and hyphens starting with a letter";

  /** Parses {@code 25}, {@code 0025}, {@code Pikachu} or {@code mr-mime}. */
  static PokemonIdentifier parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new InvalidFieldException(FIELD, "is required");
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (Patterns.NUMBER.matcher(value).matches()) {
      long number = Long.parseLong(value);
      if (number < 1 || number > MAX_NUMBER) {
        throw new InvalidFieldException(FIELD, RULE);
      }
      return new ById(new PokemonId((int) number));
    }
    if (Patterns.NAME.matcher(value).matches()) {
      return new ByName(value);
    }
    throw new InvalidFieldException(FIELD, RULE);
  }

  /** The path segment PokeAPI understands ({@code 25} or {@code pikachu}). */
  String value();

  /** By Pokédex number. */
  record ById(PokemonId id) implements PokemonIdentifier {
    @Override
    public String value() {
      return String.valueOf(id.value());
    }
  }

  /** By lower-case name. */
  record ByName(String name) implements PokemonIdentifier {
    @Override
    public String value() {
      return name;
    }
  }

  /** Compiled patterns (interfaces cannot hold private constants). */
  final class Patterns {
    static final Pattern NUMBER = Pattern.compile("\\d{1,9}");
    static final Pattern NAME = Pattern.compile("[a-z][a-z0-9-]{0,49}");

    private Patterns() {}
  }
}
