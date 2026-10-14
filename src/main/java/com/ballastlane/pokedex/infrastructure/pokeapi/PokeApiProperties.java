package com.ballastlane.pokedex.infrastructure.pokeapi;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PokeAPI client settings ({@code app.pokeapi.*}). Value object with safe defaults: a wrong value
 * stops the application at startup instead of producing a client that hangs or floods PokeAPI.
 */
@ConfigurationProperties("app.pokeapi")
public record PokeApiProperties(
    String baseUrl,
    Duration connectTimeout,
    Duration readTimeout,
    Integer maxConcurrency,
    String userAgent) {

  public static final int MAX_CONCURRENCY_LIMIT = 64;

  public PokeApiProperties {
    if (baseUrl == null || !baseUrl.matches("https?://\\S+")) {
      throw new IllegalArgumentException(
          "app.pokeapi.base-url (POKEAPI_BASE_URL) must be an http(s) URL, got '" + baseUrl + "'");
    }
    connectTimeout = positive("connect-timeout", connectTimeout, Duration.ofSeconds(2));
    readTimeout = positive("read-timeout", readTimeout, Duration.ofSeconds(5));
    maxConcurrency = maxConcurrency == null ? 8 : maxConcurrency;
    if (maxConcurrency < 1 || maxConcurrency > MAX_CONCURRENCY_LIMIT) {
      throw new IllegalArgumentException(
          "app.pokeapi.max-concurrency must be between 1 and " + MAX_CONCURRENCY_LIMIT);
    }
    userAgent =
        userAgent == null || userAgent.isBlank()
            ? "pokedex-api (+https://github.com/aleferod/pokedex-api)"
            : userAgent;
  }

  private static Duration positive(String name, Duration value, Duration fallback) {
    Duration resolved = value == null ? fallback : value;
    if (resolved.isZero() || resolved.isNegative()) {
      throw new IllegalArgumentException("app.pokeapi." + name + " must be positive");
    }
    return resolved;
  }
}
