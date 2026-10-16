package com.ballastlane.pokedex.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.cache.CachingPokemonCatalog;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.infrastructure.pokeapi.PokeApiConfiguration;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Policy;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

class CatalogCacheConfigurationTest {

  private final ApplicationContextRunner context =
      new ApplicationContextRunner()
          .withUserConfiguration(CatalogCacheConfiguration.class, PokeApiConfiguration.class)
          .withPropertyValues(
              "app.pokeapi.base-url=http://localhost:1/api/v2",
              "app.catalog-cache.ttl=PT10M",
              "app.catalog-cache.max-entries=50");

  @Test
  void should_haveExactlyTheTwoCatalogCaches_when_started() {
    context.run(
        started -> {
          CacheManager manager = started.getBean(CacheManager.class);
          assertThat(manager.getCacheNames())
              .containsExactlyInAnyOrder(
                  CachingPokemonCatalog.PAGES,
                  CachingPokemonCatalog.DETAILS,
                  CachingPokemonCatalog.NAMES);
          assertThat(manager.getCache("anything-else")).isNull();
        });
  }

  @Test
  void should_expireAndBoundEveryCacheAsConfigured_when_started() {
    context.run(
        started -> {
          CacheManager manager = started.getBean(CacheManager.class);
          for (String name :
              List.of(
                  CachingPokemonCatalog.PAGES,
                  CachingPokemonCatalog.DETAILS,
                  CachingPokemonCatalog.NAMES)) {
            Cache<Object, Object> cache = ((CaffeineCache) manager.getCache(name)).getNativeCache();
            Policy<Object, Object> policy = cache.policy();
            assertThat(policy.expireAfterWrite())
                .hasValueSatisfying(
                    expiry ->
                        assertThat(expiry.getExpiresAfter()).isEqualTo(Duration.ofMinutes(10)));
            assertThat(policy.eviction())
                .hasValueSatisfying(eviction -> assertThat(eviction.getMaximum()).isEqualTo(50));
            assertThat(policy.isRecordingStats()).isTrue();
          }
        });
  }

  @Test
  void should_exposeTheCachedCatalogAsThePort_when_started() {
    context.run(
        started ->
            assertThat(started.getBean(PokemonCatalogPort.class))
                .isInstanceOf(CachingPokemonCatalog.class));
  }
}
