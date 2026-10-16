package com.ballastlane.pokedex.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** PokeAPI paginated list: total {@code count} and one page of {@code results}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResourceListDto(long count, List<NamedResourceDto> results) {

  public ResourceListDto {
    results = results == null ? List.of() : List.copyOf(results);
  }
}
