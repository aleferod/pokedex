package com.ballastlane.pokedex.adapter.out.pokeapi;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.NamedResourceDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonSpeciesDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.ResourceListDto;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.List;

/**
 * Adapter: {@link PokemonCatalogPort} on PokeAPI. One list call gives the page and the total; each
 * entry then needs its {@code /pokemon} and {@code /pokemon-species} resources, fetched in parallel
 * through the shared {@link BoundedFanOut}. Any upstream failure fails the whole page with {@link
 * CatalogUnavailableException}, never a silently partial page.
 */
public class PokeApiCatalogAdapter implements PokemonCatalogPort {

  private final PokeApiClient client;
  private final BoundedFanOut fanOut;

  public PokeApiCatalogAdapter(PokeApiClient client, BoundedFanOut fanOut) {
    this.client = client;
    this.fanOut = fanOut;
  }

  @Override
  public Page<PokemonSummary> findPage(PageRequest request) {
    try {
      ResourceListDto list = client.listPokemon(request.offset(), request.size());
      List<PokemonSummary> summaries = fanOut.map(list.results(), this::summaryOf);
      return Page.of(summaries, request, list.count());
    } catch (PokeApiUnavailableException | IllegalArgumentException upstreamFailure) {
      throw new CatalogUnavailableException(
          "The Pokémon catalog source is unavailable", upstreamFailure);
    }
  }

  private PokemonSummary summaryOf(NamedResourceDto entry) {
    PokemonDto pokemon = client.pokemon(PokeApiMapper.idFromUrl(entry.url()));
    PokemonSpeciesDto species = client.species(PokeApiMapper.idFromUrl(pokemon.species().url()));
    return PokeApiMapper.toSummary(pokemon, species);
  }
}
