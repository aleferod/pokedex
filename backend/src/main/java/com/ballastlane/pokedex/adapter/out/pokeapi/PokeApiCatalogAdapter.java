package com.ballastlane.pokedex.adapter.out.pokeapi;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionChainDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.NamedResourceDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonSpeciesDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.ResourceListDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.evolution.EvolutionConditions;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.CatalogName;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.List;
import java.util.Optional;

/**
 * Adapter: {@link PokemonCatalogPort} on PokeAPI. One list call gives the page and the total; each
 * entry then needs its {@code /pokemon} and {@code /pokemon-species} resources, fetched in parallel
 * through the shared {@link BoundedFanOut}. Any upstream failure fails the whole page with {@link
 * CatalogUnavailableException}, never a silently partial page. A detail needs three sequential
 * calls (Pokémon, species, evolution chain); only a 404 on the Pokémon itself means "no such
 * Pokémon".
 */
public class PokeApiCatalogAdapter implements PokemonCatalogPort {

  /** More entries than PokeAPI lists (1351 in 2026), so one request returns them all. */
  static final int ALL_ENTRIES = 100_000;

  private final PokeApiClient client;
  private final BoundedFanOut fanOut;
  private final PokeApiDetailMapper detailMapper =
      new PokeApiDetailMapper(new EvolutionConditions());

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
      throw unavailable(upstreamFailure);
    }
  }

  @Override
  public Optional<PokemonDetail> findDetail(PokemonIdentifier identifier) {
    PokemonDto pokemon;
    try {
      pokemon = client.pokemon(identifier.value());
    } catch (PokeApiNotFoundException unknown) {
      return Optional.empty();
    } catch (PokeApiUnavailableException upstreamFailure) {
      throw unavailable(upstreamFailure);
    }
    try {
      PokemonSpeciesDto species = client.species(PokeApiMapper.idFromUrl(pokemon.species().url()));
      if (species.evolutionChain() == null) {
        throw new PokeApiUnavailableException("Species has no evolution chain", null);
      }
      EvolutionChainDto chain =
          client.evolutionChain(PokeApiMapper.idFromUrl(species.evolutionChain().url()));
      return Optional.of(detailMapper.toDetail(pokemon, species, chain));
    } catch (PokeApiUnavailableException | IllegalArgumentException inconsistent) {
      throw unavailable(inconsistent);
    }
  }

  private static CatalogUnavailableException unavailable(RuntimeException cause) {
    return new CatalogUnavailableException("The Pokémon catalog source is unavailable", cause);
  }

  private PokemonSummary summaryOf(NamedResourceDto entry) {
    return summaryOf(PokeApiMapper.idFromUrl(entry.url()));
  }

  private PokemonSummary summaryOf(int id) {
    PokemonDto pokemon = client.pokemon(id);
    PokemonSpeciesDto species = client.species(PokeApiMapper.idFromUrl(pokemon.species().url()));
    return PokeApiMapper.toSummary(pokemon, species);
  }

  /** The whole name index in one request (about 90 KB): every entry PokeAPI lists. */
  @Override
  public List<CatalogName> findAllNames() {
    try {
      return client.listPokemon(0, ALL_ENTRIES).results().stream()
          .map(
              entry ->
                  new CatalogName(
                      new PokemonId(PokeApiMapper.idFromUrl(entry.url())), entry.name()))
          .toList();
    } catch (PokeApiUnavailableException | IllegalArgumentException upstreamFailure) {
      throw unavailable(upstreamFailure);
    }
  }

  @Override
  public List<PokemonSummary> findSummaries(List<PokemonId> ids) {
    try {
      return fanOut.map(ids, id -> summaryOf(id.value()));
    } catch (PokeApiUnavailableException | IllegalArgumentException upstreamFailure) {
      throw unavailable(upstreamFailure);
    }
  }
}
