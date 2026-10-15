package com.ballastlane.pokedex.infrastructure.pokeapi;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PokeAPI client settings ({@code app.pokeapi.*}): base URL, timeouts, concurrency, retry and
 * circuit breaker. Value object with safe defaults: a wrong value stops the application at startup
 * instead of producing a client that hangs or floods PokeAPI.
 */
@ConfigurationProperties("app.pokeapi")
public record PokeApiProperties(
    String baseUrl,
    Duration connectTimeout,
    Duration readTimeout,
    Integer maxConcurrency,
    String userAgent,
    Retry retry,
    CircuitBreaker circuitBreaker) {

  /** {@code app.pokeapi.retry.*}: attempts per request (1 disables retries) and first wait. */
  public record Retry(Integer maxAttempts, Duration initialBackoff) {

    public static final int MAX_ATTEMPTS_LIMIT = 10;

    public Retry {
      maxAttempts = maxAttempts == null ? 3 : maxAttempts;
      if (maxAttempts < 1 || maxAttempts > MAX_ATTEMPTS_LIMIT) {
        throw new IllegalArgumentException(
            "app.pokeapi.retry.max-attempts must be between 1 and " + MAX_ATTEMPTS_LIMIT);
      }
      initialBackoff = positive("retry.initial-backoff", initialBackoff, Duration.ofMillis(200));
    }
  }

  /** {@code app.pokeapi.circuit-breaker.*}: when to stop calling PokeAPI, and for how long. */
  public record CircuitBreaker(
      Float failureRateThreshold,
      Integer minimumCalls,
      Integer slidingWindow,
      Duration openDuration) {

    public CircuitBreaker {
      failureRateThreshold = failureRateThreshold == null ? 50f : failureRateThreshold;
      if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
        throw new IllegalArgumentException(
            "app.pokeapi.circuit-breaker.failure-rate-threshold must be above 0 and at most 100");
      }
      slidingWindow = slidingWindow == null ? 20 : slidingWindow;
      if (slidingWindow < 1) {
        throw new IllegalArgumentException(
            "app.pokeapi.circuit-breaker.sliding-window must be at least 1");
      }
      minimumCalls = minimumCalls == null ? 10 : minimumCalls;
      if (minimumCalls < 1 || minimumCalls > slidingWindow) {
        throw new IllegalArgumentException(
            "app.pokeapi.circuit-breaker.minimum-calls must be between 1 and the sliding window ("
                + slidingWindow
                + ")");
      }
      openDuration =
          positive("circuit-breaker.open-duration", openDuration, Duration.ofSeconds(30));
    }
  }

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
    retry = retry == null ? new Retry(null, null) : retry;
    circuitBreaker =
        circuitBreaker == null ? new CircuitBreaker(null, null, null, null) : circuitBreaker;
  }

  private static Duration positive(String name, Duration value, Duration fallback) {
    Duration resolved = value == null ? fallback : value;
    if (resolved.isZero() || resolved.isNegative()) {
      throw new IllegalArgumentException("app.pokeapi." + name + " must be positive");
    }
    return resolved;
  }
}
