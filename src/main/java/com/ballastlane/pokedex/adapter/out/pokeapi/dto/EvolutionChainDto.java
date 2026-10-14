package com.ballastlane.pokedex.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** PokeAPI {@code /evolution-chain/{id}}: a tree of chain links from the base form. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionChainDto(int id, ChainLink chain) {

  /** One stage: its species, how it is reached, and the stages it evolves into. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChainLink(
      NamedResourceDto species,
      @JsonProperty("evolution_details") List<EvolutionDetailDto> evolutionDetails,
      @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {

    public ChainLink {
      evolutionDetails = evolutionDetails == null ? List.of() : List.copyOf(evolutionDetails);
      evolvesTo = evolvesTo == null ? List.of() : List.copyOf(evolvesTo);
    }
  }
}
