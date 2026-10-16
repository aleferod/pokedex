package com.ballastlane.pokedex.adapter.out.pokeapi.evolution;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionDetailDto;
import java.util.List;
import java.util.Map;

/**
 * Picks how to describe a stage's evolution. PokeAPI lists one method per game, many tied to a
 * specific in-game location, so the first location-free method is described (else the first one),
 * by the {@link EvolutionConditionStrategy} registered for its trigger. Supporting a new trigger
 * means one new strategy class and one map entry.
 */
public final class EvolutionConditions {

  private final Map<String, EvolutionConditionStrategy> strategies =
      Map.of(
          "level-up", new LevelUpCondition(),
          "use-item", new UseItemCondition(),
          "trade", new TradeCondition());
  private final EvolutionConditionStrategy fallback = new FallbackCondition();

  /** The condition for reaching a stage; {@code null} for a base form (no methods). */
  public String describe(List<EvolutionDetailDto> details) {
    if (details == null || details.isEmpty()) {
      return null;
    }
    EvolutionDetailDto chosen =
        details.stream()
            .filter(detail -> !Names.present(detail.location()))
            .findFirst()
            .orElse(details.getFirst());
    String trigger = Names.present(chosen.trigger()) ? chosen.trigger().name() : "";
    return strategies.getOrDefault(trigger, fallback).describe(chosen);
  }
}
