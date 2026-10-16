package com.ballastlane.pokedex.domain.pokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;

/** The six base stats shown on the detail view. */
public record BaseStats(
    int hp, int attack, int defense, int specialAttack, int specialDefense, int speed) {

  public BaseStats {
    requireNonNegative("hp", hp);
    requireNonNegative("attack", attack);
    requireNonNegative("defense", defense);
    requireNonNegative("specialAttack", specialAttack);
    requireNonNegative("specialDefense", specialDefense);
    requireNonNegative("speed", speed);
  }

  private static void requireNonNegative(String stat, int value) {
    if (value < 0) {
      throw new InvalidFieldException("stats." + stat, "must not be negative");
    }
  }
}
