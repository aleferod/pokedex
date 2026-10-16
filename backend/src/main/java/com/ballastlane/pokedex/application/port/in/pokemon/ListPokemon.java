package com.ballastlane.pokedex.application.port.in.pokemon;

import com.ballastlane.pokedex.domain.pokemon.PokemonNameQuery;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;

/** Use case (US01): browse the Pokémon catalog page by page. */
public interface ListPokemon {

  /**
   * @throws com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException the
   *     catalog source cannot answer right now
   */
  Page<PokemonSummary> list(PageRequest request);

  /**
   * The catalog entries whose name contains {@code query}, in catalog order, a page at a time.
   *
   * @throws com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException the
   *     catalog source cannot answer right now
   */
  Page<PokemonSummary> search(PokemonNameQuery query, PageRequest request);
}
