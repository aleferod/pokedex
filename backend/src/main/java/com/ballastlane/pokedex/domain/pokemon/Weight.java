package com.ballastlane.pokedex.domain.pokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.math.BigDecimal;

/**
 * Value object: a Pokémon's weight. Stored in hectograms, PokeAPI's unit, so the conversion to
 * kilograms is exact (69 hg = 6.9 kg) and never suffers from floating-point rounding.
 */
public record Weight(int hectograms) {

  public Weight {
    if (hectograms < 0) {
      throw new InvalidFieldException("weight", "must not be negative");
    }
  }

  public static Weight ofHectograms(int hectograms) {
    return new Weight(hectograms);
  }

  /** Kilograms with one decimal place. */
  public BigDecimal kilograms() {
    return BigDecimal.valueOf(hectograms, 1);
  }
}
