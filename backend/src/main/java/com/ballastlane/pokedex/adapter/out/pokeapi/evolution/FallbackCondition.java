package com.ballastlane.pokedex.adapter.out.pokeapi.evolution;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionDetailDto;

/** Any trigger without a dedicated strategy: its humanized name ("Tower Of Darkness"). */
final class FallbackCondition implements EvolutionConditionStrategy {

  @Override
  public String describe(EvolutionDetailDto detail) {
    return Names.present(detail.trigger()) ? Names.humanize(detail.trigger()) : "Special condition";
  }
}
