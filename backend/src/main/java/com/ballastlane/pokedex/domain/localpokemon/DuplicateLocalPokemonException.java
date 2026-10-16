package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.shared.DomainException;

/** A local record for this PokeAPI number already exists. */
public class DuplicateLocalPokemonException extends DomainException {

  private static final long serialVersionUID = 1L;

  public DuplicateLocalPokemonException(PokemonId pokeapiId) {
    super("Pokémon #" + pokeapiId.value() + " is already stored locally.");
  }
}
