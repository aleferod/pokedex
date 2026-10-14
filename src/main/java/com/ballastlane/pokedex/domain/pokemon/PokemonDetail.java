package com.ballastlane.pokedex.domain.pokemon;

import java.util.List;
import java.util.Objects;

/**
 * Everything the detail view (US02) shows for one Pokémon, plus the default sprite kept by the
 * local replica. {@code spriteUrl}, {@code imageUrl}, {@code category} and {@code description} are
 * {@code null} when the source has no value for them.
 */
public record PokemonDetail(
    PokemonId id,
    String name,
    String spriteUrl,
    String imageUrl,
    String category,
    String description,
    List<String> types,
    Height height,
    Weight weight,
    List<String> abilities,
    BaseStats stats,
    EvolutionStage evolutionChain) {

  public PokemonDetail {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(height, "height");
    Objects.requireNonNull(weight, "weight");
    Objects.requireNonNull(stats, "stats");
    Objects.requireNonNull(evolutionChain, "evolutionChain");
    types = types == null ? List.of() : List.copyOf(types);
    abilities = abilities == null ? List.of() : List.copyOf(abilities);
  }
}
