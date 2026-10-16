package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.pokemon.BaseStats;
import com.ballastlane.pokedex.domain.pokemon.Height;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.Weight;
import java.util.List;
import java.util.Objects;

/**
 * Value object: the PokeAPI fields kept in the local copy. Replaced as a whole by every sync and
 * never editable by clients.
 */
public record UpstreamData(
    String name,
    String spriteUrl,
    String imageUrl,
    String category,
    List<String> types,
    Height height,
    Weight weight,
    List<String> abilities,
    BaseStats stats) {

  public UpstreamData {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(height, "height");
    Objects.requireNonNull(weight, "weight");
    Objects.requireNonNull(stats, "stats");
    types = types == null ? List.of() : List.copyOf(types);
    abilities = abilities == null ? List.of() : List.copyOf(abilities);
  }

  public static UpstreamData from(PokemonDetail detail) {
    return new UpstreamData(
        detail.name(),
        detail.spriteUrl(),
        detail.imageUrl(),
        detail.category(),
        detail.types(),
        detail.height(),
        detail.weight(),
        detail.abilities(),
        detail.stats());
  }
}
