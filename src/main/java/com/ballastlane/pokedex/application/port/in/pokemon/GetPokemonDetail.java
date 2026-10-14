package com.ballastlane.pokedex.application.port.in.pokemon;

import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;

/** Use case (US02): the detailed view of one Pokémon. */
public interface GetPokemonDetail {

  /**
   * @throws com.ballastlane.pokedex.domain.pokemon.PokemonNotFoundException no Pokémon matches
   * @throws com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException the
   *     catalog source cannot answer right now
   */
  PokemonDetail get(PokemonIdentifier identifier);
}
