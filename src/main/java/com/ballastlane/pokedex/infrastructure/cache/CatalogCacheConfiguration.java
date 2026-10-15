package com.ballastlane.pokedex.infrastructure.cache;

import com.ballastlane.pokedex.adapter.out.cache.CachingPokemonCatalog;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The catalog caches: Spring's cache abstraction on Caffeine (ADR-0003). Static, so a typo in a
 * cache name fails instead of silently creating an unbounded cache. Statistics are recorded for
 * Micrometer. No {@code @EnableCaching}: {@link CachingPokemonCatalog} uses the caches directly.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CatalogCacheProperties.class)
public class CatalogCacheConfiguration {

  @Bean
  CacheManager cacheManager(CatalogCacheProperties properties) {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setAllowNullValues(false);
    manager.setCacheNames(List.of());
    for (String name : List.of(CachingPokemonCatalog.PAGES, CachingPokemonCatalog.DETAILS)) {
      manager.registerCustomCache(
          name,
          Caffeine.newBuilder()
              .expireAfterWrite(properties.ttl())
              .maximumSize(properties.maxEntries())
              .recordStats()
              .build());
    }
    return manager;
  }
}
