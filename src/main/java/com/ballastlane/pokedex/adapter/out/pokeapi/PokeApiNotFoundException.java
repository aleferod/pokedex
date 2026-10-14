package com.ballastlane.pokedex.adapter.out.pokeapi;

/**
 * PokeAPI answered 404. A special case of "could not get it": callers that do not care (catalog
 * pages) treat it as unavailable; the detail lookup turns it into "no such Pokémon".
 */
class PokeApiNotFoundException extends PokeApiUnavailableException {

  private static final long serialVersionUID = 1L;

  PokeApiNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
