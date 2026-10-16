package com.ballastlane.pokedex.domain.pokemon;

import java.util.Objects;

/** One entry of the catalog's name index: a Pokémon's number and its (lower-case) name. */
public record CatalogName(PokemonId id, String name) {

  public CatalogName {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
  }
}
