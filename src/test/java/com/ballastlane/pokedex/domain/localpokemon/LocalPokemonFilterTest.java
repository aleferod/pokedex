package com.ballastlane.pokedex.domain.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocalPokemonFilterTest {

  @Test
  void should_normalizeLikeInternalTags_when_tagIsGiven() {
    assertThat(new LocalPokemonFilter("  STARTER ", null).tag()).isEqualTo("starter");
  }

  @Test
  void should_trimAndLowerCaseTheName_when_nameIsGiven() {
    assertThat(new LocalPokemonFilter(null, " SAUR ").name()).isEqualTo("saur");
  }

  @Test
  void should_ignoreTheCriterion_when_itIsBlank() {
    assertThat(new LocalPokemonFilter("  ", "")).isEqualTo(LocalPokemonFilter.none());
    assertThat(LocalPokemonFilter.none().tag()).isNull();
    assertThat(LocalPokemonFilter.none().name()).isNull();
  }
}
