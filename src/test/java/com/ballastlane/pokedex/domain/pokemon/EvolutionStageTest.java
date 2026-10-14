package com.ballastlane.pokedex.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvolutionStageTest {

  @Test
  void should_treatMissingNextStagesAsFinalStage_when_null() {
    EvolutionStage ditto = new EvolutionStage(new PokemonId(132), "ditto", null, null);

    assertThat(ditto.evolvesTo()).isEmpty();
  }

  @Test
  void should_keepTheTreeImmutable_when_sourceListChanges() {
    // Given
    List<EvolutionStage> next = new ArrayList<>();
    next.add(new EvolutionStage(new PokemonId(134), "vaporeon", "Use Water Stone", List.of()));
    EvolutionStage eevee = new EvolutionStage(new PokemonId(133), "eevee", null, next);

    // When
    next.add(new EvolutionStage(new PokemonId(135), "jolteon", "Use Thunder Stone", List.of()));

    // Then
    assertThat(eevee.evolvesTo()).extracting(EvolutionStage::name).containsExactly("vaporeon");
    assertThatThrownBy(() -> eevee.evolvesTo().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
