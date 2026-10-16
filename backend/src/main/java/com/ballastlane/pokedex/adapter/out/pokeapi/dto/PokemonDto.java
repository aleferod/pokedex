package com.ballastlane.pokedex.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** PokeAPI {@code /pokemon/{idOrName}}: only the members the catalog and detail views use. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDto(
    int id,
    String name,
    int weight,
    Sprites sprites,
    List<AbilitySlot> abilities,
    NamedResourceDto species,
    int height,
    List<TypeSlot> types,
    List<StatSlot> stats) {

  public PokemonDto {
    abilities = abilities == null ? List.of() : List.copyOf(abilities);
    types = types == null ? List.of() : List.copyOf(types);
    stats = stats == null ? List.of() : List.copyOf(stats);
  }

  /** Sprite URLs; any of them may be {@code null}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Sprites(@JsonProperty("front_default") String frontDefault, Other other) {}

  /** {@code sprites.other}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Other(@JsonProperty("official-artwork") Artwork officialArtwork) {}

  /** {@code sprites.other.official-artwork}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Artwork(@JsonProperty("front_default") String frontDefault) {}

  /** One ability in its slot; hidden abilities have slot 3. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AbilitySlot(
      int slot, @JsonProperty("is_hidden") boolean hidden, NamedResourceDto ability) {}

  /** One type in its slot. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TypeSlot(int slot, NamedResourceDto type) {}

  /** One base stat ({@code hp}, {@code attack}, ..., {@code speed}). */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StatSlot(@JsonProperty("base_stat") int baseStat, NamedResourceDto stat) {}
}
