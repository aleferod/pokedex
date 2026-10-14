package com.ballastlane.pokedex.infrastructure.usecase;

import com.ballastlane.pokedex.application.port.in.localpokemon.GetLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncCatalogPage;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncPokemon;
import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.application.usecase.localpokemon.GetLocalPokemonUseCase;
import com.ballastlane.pokedex.application.usecase.localpokemon.SyncCatalogPageUseCase;
import com.ballastlane.pokedex.application.usecase.localpokemon.SyncPokemonUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free local-replica use cases to their ports and to the catalog use cases. */
@Configuration(proxyBeanMethods = false)
public class LocalPokemonUseCaseConfiguration {

  @Bean
  SyncPokemon syncPokemon(
      GetPokemonDetail getPokemonDetail, LocalPokemonRepositoryPort repository, Clock clock) {
    return new SyncPokemonUseCase(getPokemonDetail, repository, clock);
  }

  @Bean
  SyncCatalogPage syncCatalogPage(ListPokemon listPokemon, SyncPokemon syncPokemon) {
    return new SyncCatalogPageUseCase(listPokemon, syncPokemon);
  }

  @Bean
  GetLocalPokemon getLocalPokemon(LocalPokemonRepositoryPort repository) {
    return new GetLocalPokemonUseCase(repository);
  }
}
