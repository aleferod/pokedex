package com.ballastlane.pokedex.domain.pokemon;

import java.util.List;
import java.util.Objects;

/**
 * What the catalog shows for one Pokémon (US01): sprite, category, weight and abilities. {@code
 * spriteUrl} and {@code category} are {@code null} when the source has no value for them.
 */
public record PokemonSummary(
    PokemonId id,
    String name,
    String spriteUrl,
    String category,
    Weight weight,
    List<String> abilities) {

  public PokemonSummary {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(weight, "weight");
    abilities = abilities == null ? List.of() : List.copyOf(abilities);
  }
}
