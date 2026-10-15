package com.ballastlane.pokedex.infrastructure.pokeapi;

import com.ballastlane.pokedex.adapter.out.cache.CachingPokemonCatalog;
import com.ballastlane.pokedex.adapter.out.pokeapi.BoundedFanOut;
import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiCatalogAdapter;
import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiClient;
import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiResilience;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import java.util.Objects;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the PokeAPI adapter: one client with retry and circuit breaker, one shared fan-out
 * (instance-wide cap), and the port the use cases see, which is the adapter behind the catalog
 * cache (Decorator). The undecorated adapter is deliberately not a bean, so nothing can bypass the
 * cache by accident.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PokeApiProperties.class)
public class PokeApiConfiguration {

  @Bean
  PokeApiClient pokeApiClient(PokeApiProperties properties) {
    return PokeApiClient.create(
        properties.baseUrl(),
        properties.connectTimeout(),
        properties.readTimeout(),
        properties.userAgent(),
        new PokeApiResilience(resilience(properties)));
  }

  @Bean
  BoundedFanOut pokeApiFanOut(PokeApiProperties properties) {
    return new BoundedFanOut(properties.maxConcurrency());
  }

  @Bean
  PokemonCatalogPort pokemonCatalog(
      PokeApiClient pokeApiClient, BoundedFanOut pokeApiFanOut, CacheManager cacheManager) {
    return new CachingPokemonCatalog(
        new PokeApiCatalogAdapter(pokeApiClient, pokeApiFanOut),
        cache(cacheManager, CachingPokemonCatalog.PAGES),
        cache(cacheManager, CachingPokemonCatalog.DETAILS));
  }

  private static PokeApiResilience.Settings resilience(PokeApiProperties properties) {
    return new PokeApiResilience.Settings(
        properties.retry().maxAttempts(),
        properties.retry().initialBackoff(),
        properties.circuitBreaker().failureRateThreshold(),
        properties.circuitBreaker().minimumCalls(),
        properties.circuitBreaker().slidingWindow(),
        properties.circuitBreaker().openDuration());
  }

  private static Cache cache(CacheManager cacheManager, String name) {
    return Objects.requireNonNull(cacheManager.getCache(name), () -> "Missing cache " + name);
  }
}
