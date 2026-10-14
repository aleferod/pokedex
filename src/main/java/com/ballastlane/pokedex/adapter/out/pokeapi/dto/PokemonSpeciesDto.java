package com.ballastlane.pokedex.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** PokeAPI {@code /pokemon-species/{id}}: only the members the catalog uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonSpeciesDto(int id, String name, List<Genus> genera) {

  public PokemonSpeciesDto {
    genera = genera == null ? List.of() : List.copyOf(genera);
  }

  /** The species' genus ("Seed Pokémon") in one language. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Genus(String genus, NamedResourceDto language) {}
}
