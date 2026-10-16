package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;

/** Use case (US04): remove a local record. */
public interface DeleteLocalPokemon {

  /**
   * @throws com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException unknown id
   * @throws com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException the record moved
   *     past the expected version
   */
  void delete(LocalPokemonId id, ExpectedVersion expected);
}
