package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.UUID;

/** Value object: identity of a locally stored Pokémon (independent of its PokeAPI number). */
public record LocalPokemonId(UUID value) {

  public LocalPokemonId {
    if (value == null) {
      throw new InvalidFieldException("id", "is required");
    }
  }

  public static LocalPokemonId random() {
    return new LocalPokemonId(UUID.randomUUID());
  }

  public static LocalPokemonId parse(String value) {
    try {
      return new LocalPokemonId(UUID.fromString(value));
    } catch (IllegalArgumentException | NullPointerException notAUuid) {
      throw new InvalidFieldException("id", "must be a UUID");
    }
  }
}
