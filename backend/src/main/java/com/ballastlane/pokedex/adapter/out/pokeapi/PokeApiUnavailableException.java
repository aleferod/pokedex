package com.ballastlane.pokedex.adapter.out.pokeapi;

/** PokeAPI could not be reached, timed out, or answered with an error. */
class PokeApiUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final boolean retryable;

  PokeApiUnavailableException(String message, Throwable cause) {
    this(message, cause, false);
  }

  PokeApiUnavailableException(String message, Throwable cause, boolean retryable) {
    super(message, cause);
    this.retryable = retryable;
  }

  /** Whether another attempt may succeed: set by {@link PokeApiClient} for transient failures. */
  boolean retryable() {
    return retryable;
  }
}
