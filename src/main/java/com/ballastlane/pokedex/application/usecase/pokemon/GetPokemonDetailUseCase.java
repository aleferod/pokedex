package com.ballastlane.pokedex.application.usecase.pokemon;

import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonNotFoundException;

/**
 * Loads one Pokémon's detail. Keeps "no such Pokémon" (an empty answer, 404 to clients) apart from
 * "cannot tell right now" (the port's exception, 503 to clients).
 */
public class GetPokemonDetailUseCase implements GetPokemonDetail {

  private final PokemonCatalogPort catalog;

  public GetPokemonDetailUseCase(PokemonCatalogPort catalog) {
    this.catalog = catalog;
  }

  @Override
  public PokemonDetail get(PokemonIdentifier identifier) {
    return catalog
        .findDetail(identifier)
        .orElseThrow(() -> new PokemonNotFoundException(identifier));
  }

  @Override
  public PokemonDetail getFresh(PokemonIdentifier identifier) {
    return catalog
        .findFreshDetail(identifier)
        .orElseThrow(() -> new PokemonNotFoundException(identifier));
  }
}
