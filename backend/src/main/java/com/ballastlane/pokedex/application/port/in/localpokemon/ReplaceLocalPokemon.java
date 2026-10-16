package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;

/** Use case (US04): replace every proprietary field; values left out are cleared. */
public interface ReplaceLocalPokemon {

  /**
   * @throws com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException unknown id
   * @throws com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException the record moved
   *     past the expected version
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldsException the values break the rules
   */
  LocalPokemon replace(LocalPokemonId id, ExpectedVersion expected, ProprietaryValues values);
}
