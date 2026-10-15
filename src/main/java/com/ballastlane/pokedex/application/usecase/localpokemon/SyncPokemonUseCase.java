package com.ballastlane.pokedex.application.usecase.localpokemon;

import com.ballastlane.pokedex.application.port.in.localpokemon.SyncPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncResult;
import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.UpstreamData;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import java.time.Clock;

/**
 * Imports a Pokémon into the local store or refreshes it. PokeAPI is read first and fresh, never
 * from the catalog cache (reusing the US02 detail use case), so an unknown or unavailable Pokémon
 * leaves the store untouched. Races are settled by the store: a lost first-import race becomes a
 * re-sync of the winner, and a stale re-sync is retried once against the latest version.
 */
public class SyncPokemonUseCase implements SyncPokemon {

  private final GetPokemonDetail getPokemonDetail;
  private final LocalPokemonRepositoryPort repository;
  private final Clock clock;

  public SyncPokemonUseCase(
      GetPokemonDetail getPokemonDetail, LocalPokemonRepositoryPort repository, Clock clock) {
    this.getPokemonDetail = getPokemonDetail;
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public SyncResult sync(PokemonIdentifier identifier, String actor) {
    PokemonDetail detail = getPokemonDetail.getFresh(identifier);
    UpstreamData upstream = UpstreamData.from(detail);
    PokemonId pokeapiId = detail.id();
    var stored = repository.findByPokeapiId(pokeapiId);
    if (stored.isPresent()) {
      return resync(stored.get(), upstream, true);
    }
    try {
      LocalPokemon imported =
          LocalPokemon.importFrom(
              LocalPokemonId.random(), pokeapiId, upstream, actor, clock.instant());
      return new SyncResult(repository.save(imported), true);
    } catch (DuplicateLocalPokemonException lostRace) {
      LocalPokemon winner = repository.findByPokeapiId(pokeapiId).orElseThrow(() -> lostRace);
      return resync(winner, upstream, false);
    }
  }

  private SyncResult resync(LocalPokemon current, UpstreamData upstream, boolean retryIfStale) {
    try {
      return new SyncResult(repository.save(current.resyncWith(upstream, clock.instant())), false);
    } catch (StaleLocalPokemonException stale) {
      if (!retryIfStale) {
        throw stale;
      }
      LocalPokemon latest =
          repository.findByPokeapiId(current.pokeapiId()).orElseThrow(() -> stale);
      return resync(latest, upstream, false);
    }
  }
}
