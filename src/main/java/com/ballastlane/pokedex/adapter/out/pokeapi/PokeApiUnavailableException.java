package com.ballastlane.pokedex.adapter.out.pokeapi;

/** PokeAPI could not be reached, timed out, or answered with an error. */
class PokeApiUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  PokeApiUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
