package com.ballastlane.pokedex.infrastructure.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Catalog cache settings ({@code app.catalog-cache.*}): how long a page or detail is reused, and
 * how many entries each cache keeps. Validated value object: a nonsensical value stops startup.
 */
@ConfigurationProperties("app.catalog-cache")
public record CatalogCacheProperties(Duration ttl, Long maxEntries) {

  public CatalogCacheProperties {
    ttl = ttl == null ? Duration.ofHours(1) : ttl;
    maxEntries = maxEntries == null ? 2000L : maxEntries;
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException(
          "app.catalog-cache.ttl (CATALOG_CACHE_TTL) must be positive, got " + ttl);
    }
    if (maxEntries < 1) {
      throw new IllegalArgumentException(
          "app.catalog-cache.max-entries must be at least 1, got " + maxEntries);
    }
  }
}
