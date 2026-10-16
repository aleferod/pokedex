package com.ballastlane.pokedex.application.port.out.pokemon;

/**
 * Part of the {@link PokemonCatalogPort} contract: the upstream catalog cannot answer right now
 * (unreachable, timed out or failing). Callers should retry later; it is not a client error.
 */
public class CatalogUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CatalogUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
