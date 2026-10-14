package com.ballastlane.pokedex.domain.pokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;

/** Value object: a Pokédex number as used by PokeAPI (alternate forms are numbered 10001+). */
public record PokemonId(int value) {

  public PokemonId {
    if (value < 1) {
      throw new InvalidFieldException("id", "must be a positive Pokédex number");
    }
  }
}
