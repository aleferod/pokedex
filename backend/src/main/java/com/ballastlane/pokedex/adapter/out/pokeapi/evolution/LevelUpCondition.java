package com.ballastlane.pokedex.adapter.out.pokeapi.evolution;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionDetailDto;
import java.util.ArrayList;
import java.util.List;

/** {@code level-up}: "Level 16", or "Level up" followed by the conditions that apply. */
final class LevelUpCondition implements EvolutionConditionStrategy {

  @Override
  public String describe(EvolutionDetailDto detail) {
    List<String> parts = new ArrayList<>();
    parts.add(detail.minLevel() != null ? "Level " + detail.minLevel() : "Level up");
    if (detail.minHappiness() != null) {
      parts.add("with high friendship");
    }
    if (detail.minAffection() != null) {
      parts.add("with high affection");
    }
    if (detail.timeOfDay() != null && !detail.timeOfDay().isBlank()) {
      parts.add(timeOfDay(detail.timeOfDay()));
    }
    if (Names.present(detail.knownMoveType())) {
      parts.add("knowing a " + Names.humanize(detail.knownMoveType()) + " move");
    }
    if (Names.present(detail.knownMove())) {
      parts.add("knowing " + Names.humanize(detail.knownMove()));
    }
    if (Names.present(detail.heldItem())) {
      parts.add("holding " + Names.humanize(detail.heldItem()));
    }
    if (Names.present(detail.location())) {
      parts.add("at " + Names.humanize(detail.location()));
    }
    return String.join(" ", parts);
  }

  private static String timeOfDay(String value) {
    return switch (value) {
      case "day" -> "during the day";
      case "night" -> "at night";
      default -> "at " + value;
    };
  }
}
