package com.ballastlane.pokedex.domain.pokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.math.BigDecimal;

/** Value object: a Pokémon's height, stored in decimetres (PokeAPI's unit) for exact metres. */
public record Height(int decimetres) {

  public Height {
    if (decimetres < 0) {
      throw new InvalidFieldException("height", "must not be negative");
    }
  }

  public static Height ofDecimetres(int decimetres) {
    return new Height(decimetres);
  }

  /** Metres with one decimal place. */
  public BigDecimal metres() {
    return BigDecimal.valueOf(decimetres, 1);
  }
}
