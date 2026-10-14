package com.ballastlane.pokedex.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** PokeAPI {@code /pokemon/{id}}: only the members the catalog uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDto(
    int id,
    String name,
    int weight,
    Sprites sprites,
    List<AbilitySlot> abilities,
    NamedResourceDto species) {

  public PokemonDto {
    abilities = abilities == null ? List.of() : List.copyOf(abilities);
  }

  /** Sprite URLs; {@code front_default} may be {@code null}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Sprites(@JsonProperty("front_default") String frontDefault) {}

  /** One ability in its slot; hidden abilities have slot 3. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AbilitySlot(
      int slot, @JsonProperty("is_hidden") boolean hidden, NamedResourceDto ability) {}
}
