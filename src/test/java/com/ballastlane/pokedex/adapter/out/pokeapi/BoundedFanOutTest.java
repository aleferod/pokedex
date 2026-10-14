package com.ballastlane.pokedex.adapter.out.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class BoundedFanOutTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void should_returnResultsInInputOrder_when_tasksFinishInAnyOrder() {
    // Given
    BoundedFanOut fanOut = new BoundedFanOut(8);
    List<Integer> inputs = IntStream.rangeClosed(1, 20).boxed().toList();

    // When
    List<Integer> results =
        fanOut.map(
            inputs,
            input -> {
              sleep(Duration.ofMillis(ThreadLocalRandom.current().nextInt(0, 20)));
              return input * 2;
            });

    // Then
    assertThat(results).containsExactlyElementsOf(inputs.stream().map(i -> i * 2).toList());
  }

  @Test
  void should_runInParallelButNeverAboveTheCap_when_moreTasksThanPermits() {
    // Given
    BoundedFanOut fanOut = new BoundedFanOut(3);
    ConcurrencyProbe probe = new ConcurrencyProbe();

    // When
    fanOut.map(IntStream.range(0, 12).boxed().toList(), probe.task(Duration.ofMillis(50)));

    // Then
    assertThat(probe.highWaterMark()).isEqualTo(3);
  }

  @Test
  void should_shareTheCapAcrossCallers_when_requestsOverlap() {
    // Given one fan-out shared by two concurrent requests
    BoundedFanOut fanOut = new BoundedFanOut(4);
    ConcurrencyProbe probe = new ConcurrencyProbe();
    Function<Integer, Integer> task = probe.task(Duration.ofMillis(50));

    // When
    CompletableFuture<List<Integer>> first =
        CompletableFuture.supplyAsync(
            () -> fanOut.map(IntStream.range(0, 8).boxed().toList(), task));
    CompletableFuture<List<Integer>> second =
        CompletableFuture.supplyAsync(
            () -> fanOut.map(IntStream.range(0, 8).boxed().toList(), task));
    CompletableFuture.allOf(first, second).join();

    // Then
    assertThat(probe.highWaterMark()).isLessThanOrEqualTo(4);
    assertThat(first.join()).hasSize(8);
    assertThat(second.join()).hasSize(8);
  }

  @Test
  void should_rethrowTheTaskFailure_when_oneTaskFails() {
    BoundedFanOut fanOut = new BoundedFanOut(4);

    assertThatThrownBy(
            () ->
                fanOut.map(
                    IntStream.rangeClosed(1, 10).boxed().toList(),
                    input -> {
                      if (input == 5) {
                        throw new IllegalStateException("upstream failed for 5");
                      }
                      return input;
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("upstream failed for 5");
  }

  @Test
  void should_carryTheCallersCorrelationId_when_tasksRunOnOtherThreads() {
    // Given
    BoundedFanOut fanOut = new BoundedFanOut(4);
    MDC.put("correlationId", "demo-fan-out");
    ConcurrentLinkedQueue<String> seen = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<String> threads = new ConcurrentLinkedQueue<>();

    // When
    fanOut.map(
        List.of(1, 2, 3, 4, 5, 6),
        input -> {
          seen.add(String.valueOf(MDC.get("correlationId")));
          threads.add(Thread.currentThread().toString());
          return input;
        });

    // Then
    assertThat(seen).hasSize(6).containsOnly("demo-fan-out");
    assertThat(threads).doesNotContain(Thread.currentThread().toString());
    assertThat(MDC.get("correlationId")).isEqualTo("demo-fan-out");
  }

  @Test
  void should_returnEmptyList_when_thereIsNothingToDo() {
    assertThat(new BoundedFanOut(4).map(List.<Integer>of(), input -> input)).isEmpty();
  }

  @Test
  void should_reject_when_capIsNotPositive() {
    assertThatThrownBy(() -> new BoundedFanOut(0)).isInstanceOf(IllegalArgumentException.class);
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(interrupted);
    }
  }

  /** Records the highest number of tasks running at the same time. */
  private static final class ConcurrencyProbe {

    private final AtomicInteger running = new AtomicInteger();
    private final AtomicInteger highWaterMark = new AtomicInteger();

    Function<Integer, Integer> task(Duration duration) {
      return input -> {
        int now = running.incrementAndGet();
        highWaterMark.accumulateAndGet(now, Math::max);
        try {
          sleep(duration);
          return input;
        } finally {
          running.decrementAndGet();
        }
      };
    }

    int highWaterMark() {
      return highWaterMark.get();
    }
  }
}
