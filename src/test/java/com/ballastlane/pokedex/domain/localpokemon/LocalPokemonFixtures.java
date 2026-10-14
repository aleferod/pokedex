package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.pokemon.BaseStats;
import com.ballastlane.pokedex.domain.pokemon.Height;
import com.ballastlane.pokedex.domain.pokemon.Weight;
import java.util.List;

/** Shared test data for the local replica. */
public final class LocalPokemonFixtures {

  private LocalPokemonFixtures() {}

  public static UpstreamData pikachu(int weightHectograms) {
    return new UpstreamData(
        "pikachu",
        "https://img/sprites/25.png",
        "https://img/artwork/25.png",
        "Mouse Pokémon",
        List.of("electric"),
        Height.ofDecimetres(4),
        Weight.ofHectograms(weightHectograms),
        List.of("static", "lightning-rod"),
        new BaseStats(35, 55, 40, 50, 50, 90));
  }
}
