package com.ballastlane.pokedex.adapter.out.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class PokeApiResilienceTest {

  private static final Duration OPEN = Duration.ofMillis(100);

  // 3 attempts, 1 ms backoff; the circuit opens when half of the last 4 calls failed
  private final PokeApiResilience resilience =
      new PokeApiResilience(
          new PokeApiResilience.Settings(3, Duration.ofMillis(1), 50f, 4, 4, OPEN));
  private final AtomicInteger attempts = new AtomicInteger();

  @Test
  void should_answerAtOnce_when_theFirstAttemptSucceeds() {
    assertThat(resilience.call("pokemon 1", counting(() -> "bulbasaur"))).isEqualTo("bulbasaur");
    assertThat(attempts).hasValue(1);
  }

  @Test
  void should_tryAgain_when_failuresAreTransient() {
    Supplier<String> flaky =
        counting(
            () -> {
              if (attempts.get() < 3) {
                throw transientFailure();
              }
              return "bulbasaur";
            });

    assertThat(resilience.call("pokemon 1", flaky)).isEqualTo("bulbasaur");
    assertThat(attempts).hasValue(3);
  }

  @Test
  void should_rethrowTheLastFailure_when_everyAttemptFails() {
    PokeApiUnavailableException failure = transientFailure();

    assertThatThrownBy(() -> resilience.call("pokemon 1", counting(() -> throwing(failure))))
        .isSameAs(failure);
    assertThat(attempts).hasValue(3);
  }

  @Test
  void should_notTryAgain_when_theFailureIsNotTransient() {
    PokeApiUnavailableException timeout =
        new PokeApiUnavailableException("PokeAPI request failed: pokemon 1", null, false);

    assertThatThrownBy(() -> resilience.call("pokemon 1", counting(() -> throwing(timeout))))
        .isSameAs(timeout);
    assertThat(attempts).hasValue(1);
  }

  @Test
  void should_neitherRetryNorOpen_when_pokeApiAnswersNotFound() {
    for (int i = 0; i < 10; i++) {
      PokeApiNotFoundException notFound = new PokeApiNotFoundException("PokeAPI has no x", null);
      assertThatThrownBy(() -> resilience.call("pokemon x", counting(() -> throwing(notFound))))
          .isSameAs(notFound);
    }

    assertThat(attempts).hasValue(10);
    assertThat(resilience.circuitState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void should_failFastWithoutCallingPokeApi_when_theCircuitIsOpen() {
    // Given enough failures to open the circuit (each call makes three failed attempts)
    openTheCircuit();
    int attemptsSoFar = attempts.get();

    // When / Then: no attempt reaches PokeAPI, and the failure is not retryable
    assertThatThrownBy(() -> resilience.call("pokemon 1", counting(() -> "bulbasaur")))
        .isInstanceOfSatisfying(
            PokeApiUnavailableException.class,
            error -> {
              assertThat(error.retryable()).isFalse();
              assertThat(error).hasCauseInstanceOf(CallNotPermittedException.class);
              assertThat(error).hasMessageContaining("pokemon 1");
            });
    assertThat(attempts).hasValue(attemptsSoFar);
  }

  @Test
  void should_closeAgain_when_trialCallsSucceedAfterTheOpenPeriod() throws InterruptedException {
    openTheCircuit();

    Thread.sleep(OPEN.multipliedBy(2).toMillis());
    for (int trial = 0; trial < 3; trial++) {
      assertThat(resilience.call("pokemon 1", () -> "bulbasaur")).isEqualTo("bulbasaur");
    }

    assertThat(resilience.circuitState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void should_logRetriesAndCircuitChanges_when_pokeApiFails(CapturedOutput output) {
    openTheCircuit();

    assertThat(output)
        .contains("Retrying PokeAPI request after failure 1: PokeAPI request failed: pokemon 1")
        .contains("PokeAPI circuit breaker changed from CLOSED to OPEN");
  }

  private void openTheCircuit() {
    for (int call = 0; call < 2; call++) {
      assertThatThrownBy(
              () -> resilience.call("pokemon 1", counting(() -> throwing(transientFailure()))))
          .isInstanceOf(PokeApiUnavailableException.class);
    }
    assertThat(resilience.circuitState()).isEqualTo(CircuitBreaker.State.OPEN);
  }

  private <T> Supplier<T> counting(Supplier<T> request) {
    return () -> {
      attempts.incrementAndGet();
      return request.get();
    };
  }

  private static PokeApiUnavailableException transientFailure() {
    return new PokeApiUnavailableException("PokeAPI request failed: pokemon 1", null, true);
  }

  private static <T> T throwing(RuntimeException failure) {
    throw failure;
  }
}
