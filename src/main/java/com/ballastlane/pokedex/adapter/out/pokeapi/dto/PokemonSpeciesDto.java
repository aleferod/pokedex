package com.ballastlane.pokedex.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** PokeAPI {@code /pokemon-species/{id}}: only the members the catalog and detail views use. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonSpeciesDto(
    int id,
    String name,
    List<Genus> genera,
    @JsonProperty("flavor_text_entries") List<FlavorTextEntry> flavorTextEntries,
    @JsonProperty("evolution_chain") NamedResourceDto evolutionChain) {

  public PokemonSpeciesDto {
    genera = genera == null ? List.of() : List.copyOf(genera);
    flavorTextEntries = flavorTextEntries == null ? List.of() : List.copyOf(flavorTextEntries);
  }

  /** The species' genus ("Seed Pokémon") in one language. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Genus(String genus, NamedResourceDto language) {}

  /** A game's description of the species, in one language (game order: oldest first). */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FlavorTextEntry(
      @JsonProperty("flavor_text") String flavorText, NamedResourceDto language) {}
}
