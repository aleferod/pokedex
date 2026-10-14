package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;

/** Use case (US04): store a new local record, with upstream data fetched from PokeAPI. */
public interface CreateLocalPokemon {

  /**
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldsException the number or the
   *     proprietary values break the rules (every violation is listed)
   * @throws com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException the number
   *     is already stored
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldException PokeAPI has no Pokémon with
   *     this number (field {@code pokeapiId})
   * @throws com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException
   *     PokeAPI cannot answer right now
   */
  LocalPokemon create(CreateLocalPokemonCommand command, String actor);
}
