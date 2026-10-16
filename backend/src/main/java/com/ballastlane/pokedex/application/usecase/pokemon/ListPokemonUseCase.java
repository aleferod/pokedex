package com.ballastlane.pokedex.application.usecase.pokemon;

import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.CatalogName;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonNameQuery;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.List;

/**
 * Lists the catalog. Deliberately thin today: paging rules live in {@link PageRequest}, and
 * caching/resilience will wrap the {@link PokemonCatalogPort} (Decorator) without touching this
 * class. The name search's matching rule lives here too.
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

  /**
   * Filters the catalog's name index here, not in the adapter: the matching rule is the
   * application's. Only the requested page of matches is fetched in detail.
   */
  @Override
  public Page<PokemonSummary> search(PokemonNameQuery query, PageRequest request) {
    List<PokemonId> matches =
        catalog.findAllNames().stream()
            .filter(entry -> query.matches(entry.name()))
            .map(CatalogName::id)
            .toList();
    long from = request.offset();
    if (from >= matches.size()) {
      return Page.of(List.of(), request, matches.size());
    }
    List<PokemonId> pageIds =
        matches.subList((int) from, (int) Math.min(from + request.size(), matches.size()));
    return Page.of(catalog.findSummaries(pageIds), request, matches.size());
  }
}
