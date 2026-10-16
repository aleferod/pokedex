package com.ballastlane.pokedex.infrastructure.usecase;

import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.application.usecase.pokemon.GetPokemonDetailUseCase;
import com.ballastlane.pokedex.application.usecase.pokemon.ListPokemonUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free catalog use cases to their ports. */
@Configuration(proxyBeanMethods = false)
public class PokemonUseCaseConfiguration {

  @Bean
  ListPokemon listPokemon(PokemonCatalogPort pokemonCatalog) {
    return new ListPokemonUseCase(pokemonCatalog);
  }

  @Bean
  GetPokemonDetail getPokemonDetail(PokemonCatalogPort pokemonCatalog) {
    return new GetPokemonDetailUseCase(pokemonCatalog);
  }
}
