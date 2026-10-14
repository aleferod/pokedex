package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.shared.DomainException;

/** No local record has the given id. */
public class LocalPokemonNotFoundException extends DomainException {

  private static final long serialVersionUID = 1L;

  public LocalPokemonNotFoundException(LocalPokemonId id) {
    super("No local Pokémon with id " + id.value() + ".");
  }
}
