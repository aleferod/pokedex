package com.ballastlane.pokedex.infrastructure.pokeapi;

import com.ballastlane.pokedex.adapter.out.pokeapi.BoundedFanOut;
import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiCatalogAdapter;
import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiClient;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the PokeAPI adapter: one client, one shared fan-out (instance-wide cap), one port. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PokeApiProperties.class)
public class PokeApiConfiguration {

  @Bean
  PokeApiClient pokeApiClient(PokeApiProperties properties) {
    return PokeApiClient.create(
        properties.baseUrl(),
        properties.connectTimeout(),
        properties.readTimeout(),
        properties.userAgent());
  }

  @Bean
  BoundedFanOut pokeApiFanOut(PokeApiProperties properties) {
    return new BoundedFanOut(properties.maxConcurrency());
  }

  @Bean
  PokemonCatalogPort pokemonCatalog(PokeApiClient pokeApiClient, BoundedFanOut pokeApiFanOut) {
    return new PokeApiCatalogAdapter(pokeApiClient, pokeApiFanOut);
  }
}
