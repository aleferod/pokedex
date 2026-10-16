package com.ballastlane.pokedex.domain.pokemon;

import com.ballastlane.pokedex.domain.shared.DomainException;

/** No Pokémon matches a well-formed identifier. */
public class PokemonNotFoundException extends DomainException {

  private static final long serialVersionUID = 1L;

  private final String identifier;

  public PokemonNotFoundException(PokemonIdentifier identifier) {
    super("No Pokémon matches '" + identifier.value() + "'.");
    this.identifier = identifier.value();
  }

  public String identifier() {
    return identifier;
  }
}
