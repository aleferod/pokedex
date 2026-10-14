package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;

/** Use case (US03): import a Pokémon from PokeAPI into the local store, or refresh it. */
public interface SyncPokemon {

  /**
   * @throws com.ballastlane.pokedex.domain.pokemon.PokemonNotFoundException unknown Pokémon
   * @throws com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException
   *     PokeAPI cannot answer; nothing is stored or changed
   */
  SyncResult sync(PokemonIdentifier identifier, String actor);
}
