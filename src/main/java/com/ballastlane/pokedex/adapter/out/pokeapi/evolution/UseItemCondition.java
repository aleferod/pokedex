package com.ballastlane.pokedex.adapter.out.pokeapi.evolution;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionDetailDto;

/** {@code use-item}: "Use Water Stone". */
final class UseItemCondition implements EvolutionConditionStrategy {

  @Override
  public String describe(EvolutionDetailDto detail) {
    return Names.present(detail.item()) ? "Use " + Names.humanize(detail.item()) : "Use an item";
  }
}
