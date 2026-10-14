package com.ballastlane.pokedex.adapter.out.pokeapi.evolution;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionDetailDto;

/** {@code trade}: "Trade", optionally "holding Metal Coat" or "for Shelmet". */
final class TradeCondition implements EvolutionConditionStrategy {

  @Override
  public String describe(EvolutionDetailDto detail) {
    StringBuilder text = new StringBuilder("Trade");
    if (Names.present(detail.heldItem())) {
      text.append(" holding ").append(Names.humanize(detail.heldItem()));
    }
    if (Names.present(detail.tradeSpecies())) {
      text.append(" for ").append(Names.humanize(detail.tradeSpecies()));
    }
    return text.toString();
  }
}
