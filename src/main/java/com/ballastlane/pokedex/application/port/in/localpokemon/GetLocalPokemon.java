package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;

/** Use case: read one local record. */
public interface GetLocalPokemon {

  /**
   * @throws com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException unknown id
   */
  LocalPokemon get(LocalPokemonId id);
}
