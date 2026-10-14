package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryPatch;

/** Use case (US04): apply a JSON Merge Patch to the proprietary fields. */
public interface PatchLocalPokemon {

  /**
   * @throws com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException unknown id
   * @throws com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException the record moved
   *     past the expected version
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldsException the patched values break
   *     the rules
   */
  LocalPokemon patch(LocalPokemonId id, ExpectedVersion expected, ProprietaryPatch patch);
}
