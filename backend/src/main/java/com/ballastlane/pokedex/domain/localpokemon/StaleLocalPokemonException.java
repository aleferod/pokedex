package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.shared.DomainException;

/** The local record changed since it was read (optimistic-locking conflict). */
public class StaleLocalPokemonException extends DomainException {

  private static final long serialVersionUID = 1L;

  public StaleLocalPokemonException(LocalPokemonId id) {
    super("Local Pokémon " + id.value() + " was changed by someone else; reload it and retry.");
  }
}
