package com.ballastlane.pokedex.adapter.in.web.pokemon;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** The detailed view of one Pokémon (US02). */
public record PokemonDetailResponse(
    @Schema(example = "1") int id,
    @Schema(example = "bulbasaur") String name,
    @Schema(description = "Official artwork", nullable = true) String imageUrl,
    @Schema(example = "Seed Pokémon", nullable = true) String category,
    @Schema(description = "Newest English flavor text, cleaned", nullable = true)
        String description,
    @Schema(example = "[\"grass\", \"poison\"]") List<String> types,
    @Schema(example = "0.7", description = "Height in metres") BigDecimal height,
    @Schema(example = "6.9", description = "Weight in kilograms") BigDecimal weight,
    List<String> abilities,
    BaseStatsResponse stats,
    @Schema(description = "Whole evolution family as a tree, from the base form")
        EvolutionStageResponse evolutionChain) {

  /** The six base stats. */
  public record BaseStatsResponse(
      int hp, int attack, int defense, int specialAttack, int specialDefense, int speed) {}

  /** One stage of the evolution tree. */
  public record EvolutionStageResponse(
      @Schema(example = "2") int id,
      @Schema(example = "ivysaur") String name,
      @Schema(
              example = "Level 16",
              description = "How to reach this stage; null for the base form",
              nullable = true)
          String condition,
      List<EvolutionStageResponse> evolvesTo) {}
}
