package com.ballastlane.pokedex.adapter.out.pokeapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import org.slf4j.MDC;

/**
 * Bulkhead for upstream calls: runs one task per input on virtual threads, but never more than
 * {@code maxConcurrency} at once across the whole instance (one shared semaphore), so concurrent
 * requests cannot gang up on PokeAPI. Results keep the input order; the first failure cancels the
 * remaining tasks and is rethrown. The caller's MDC (correlation id) is copied into every task.
 */
public final class BoundedFanOut {

  private final Semaphore permits;

  public BoundedFanOut(int maxConcurrency) {
    if (maxConcurrency < 1) {
      throw new IllegalArgumentException("maxConcurrency must be at least 1");
    }
    this.permits = new Semaphore(maxConcurrency, true);
  }

  public <T, R> List<R> map(List<T> inputs, Function<T, R> task) {
    if (inputs.isEmpty()) {
      return List.of();
    }
    Map<String, String> callerMdc = MDC.getCopyOfContextMap();
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<R>> futures = new ArrayList<>(inputs.size());
      for (T input : inputs) {
        futures.add(executor.submit(() -> runWithPermit(input, task, callerMdc)));
      }
      return collect(futures);
    }
  }

  private <T, R> R runWithPermit(T input, Function<T, R> task, Map<String, String> callerMdc)
      throws InterruptedException {
    permits.acquire();
    try {
      if (callerMdc != null) {
        MDC.setContextMap(callerMdc);
      }
      return task.apply(input);
    } finally {
      MDC.clear();
      permits.release();
    }
  }

  private static <R> List<R> collect(List<Future<R>> futures) {
    List<R> results = new ArrayList<>(futures.size());
    for (Future<R> future : futures) {
      try {
        results.add(future.get());
      } catch (ExecutionException failed) {
        futures.forEach(other -> other.cancel(true));
        throw asUnchecked(failed.getCause());
      } catch (InterruptedException interrupted) {
        futures.forEach(other -> other.cancel(true));
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "Interrupted while waiting for upstream calls", interrupted);
      }
    }
    return results;
  }

  private static RuntimeException asUnchecked(Throwable cause) {
    if (cause instanceof RuntimeException runtime) {
      return runtime;
    }
    if (cause instanceof Error error) {
      throw error;
    }
    return new IllegalStateException(cause);
  }
}
