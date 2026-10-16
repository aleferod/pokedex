package com.ballastlane.pokedex.adapter.out.pokeapi;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resilience for every PokeAPI request, decorated explicitly with Resilience4j (no AOP): {@code
 * Retry(CircuitBreaker(request))}, so each attempt is seen by the breaker.
 *
 * <ul>
 *   <li><b>Retry</b> only what {@link PokeApiClient} marked {@linkplain
 *       PokeApiUnavailableException#retryable() retryable} (connection failures, 5xx, 429), with
 *       exponential, jittered backoff.
 *   <li><b>Circuit breaker</b>: when too many recent calls fail, stop calling PokeAPI for a while
 *       and fail fast. A 404 is a normal answer and never counts as a failure.
 * </ul>
 *
 * Retries and state changes are logged on the calling thread, so they carry the correlation id.
 */
public final class PokeApiResilience {

  private static final Logger LOG = LoggerFactory.getLogger(PokeApiResilience.class);
  private static final String NAME = "pokeapi";
  private static final int TRIAL_CALLS = 3;
  private static final double BACKOFF_MULTIPLIER = 2.0;
  private static final double BACKOFF_JITTER = 0.5;

  /** Plain settings, mapped from configuration by the infrastructure. */
  public record Settings(
      int maxAttempts,
      Duration initialBackoff,
      float failureRateThreshold,
      int minimumCalls,
      int slidingWindow,
      Duration openDuration) {}

  private final Retry retry;
  private final CircuitBreaker circuitBreaker;

  public PokeApiResilience(Settings settings) {
    retry =
        Retry.of(
            NAME,
            RetryConfig.custom()
                .maxAttempts(settings.maxAttempts())
                .intervalFunction(
                    IntervalFunction.ofExponentialRandomBackoff(
                        settings.initialBackoff(), BACKOFF_MULTIPLIER, BACKOFF_JITTER))
                .retryOnException(PokeApiResilience::isTransient)
                .build());
    circuitBreaker =
        CircuitBreaker.of(
            NAME,
            CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(settings.slidingWindow())
                .minimumNumberOfCalls(settings.minimumCalls())
                .failureRateThreshold(settings.failureRateThreshold())
                .waitDurationInOpenState(settings.openDuration())
                .permittedNumberOfCallsInHalfOpenState(TRIAL_CALLS)
                .recordException(PokeApiResilience::isFailure)
                .ignoreExceptions(PokeApiNotFoundException.class)
                .build());
    retry
        .getEventPublisher()
        .onRetry(
            event ->
                LOG.info(
                    "Retrying PokeAPI request after failure {}: {} (waiting {} ms)",
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage(),
                    event.getWaitInterval().toMillis()));
    circuitBreaker
        .getEventPublisher()
        .onStateTransition(
            event ->
                LOG.warn(
                    "PokeAPI circuit breaker changed from {} to {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()));
  }

  /**
   * Runs {@code request} under retry and circuit breaker.
   *
   * @throws PokeApiUnavailableException the last failure, or a non-retryable one when the circuit
   *     is open and PokeAPI was not called
   */
  <T> T call(String what, Supplier<T> request) {
    Supplier<T> guarded =
        Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, request));
    try {
      return guarded.get();
    } catch (CallNotPermittedException open) {
      throw new PokeApiUnavailableException(
          "PokeAPI circuit breaker is open; not calling: " + what, open, false);
    }
  }

  CircuitBreaker.State circuitState() {
    return circuitBreaker.getState();
  }

  private static boolean isTransient(Throwable failure) {
    return failure instanceof PokeApiUnavailableException unavailable && unavailable.retryable();
  }

  private static boolean isFailure(Throwable failure) {
    return failure instanceof PokeApiUnavailableException
        && !(failure instanceof PokeApiNotFoundException);
  }
}
