package com.ballastlane.pokedex.domain.pokemon;

import java.util.List;
import java.util.Objects;

/**
 * One stage of an evolution family, as a tree: the root is the base form ({@code condition} is
 * {@code null}); branching evolutions (Eevee) are several entries in {@code evolvesTo}; a final
 * stage has none. Immutable.
 */
public record EvolutionStage(
    PokemonId id, String name, String condition, List<EvolutionStage> evolvesTo) {

  public EvolutionStage {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    evolvesTo = evolvesTo == null ? List.of() : List.copyOf(evolvesTo);
  }
}
