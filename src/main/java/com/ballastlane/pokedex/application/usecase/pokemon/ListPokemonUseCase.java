package com.ballastlane.pokedex.application.usecase.pokemon;

import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;

/**
 * Lists the catalog. Deliberately thin today: paging rules live in {@link PageRequest}, and
 * caching/resilience will wrap the {@link PokemonCatalogPort} (Decorator) without touching this
 * class. Filtering or ordering rules would be added here.
 */
public class ListPokemonUseCase implements ListPokemon {

  private final PokemonCatalogPort catalog;

  public ListPokemonUseCase(PokemonCatalogPort catalog) {
    this.catalog = catalog;
  }

  @Override
  public Page<PokemonSummary> list(PageRequest request) {
    return catalog.findPage(request);
  }
}
