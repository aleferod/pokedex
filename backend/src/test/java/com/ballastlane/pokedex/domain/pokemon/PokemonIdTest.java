package com.ballastlane.pokedex.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PokemonIdTest {

  @Test
  void should_keepTheNumber_when_positive() {
    assertThat(new PokemonId(25).value()).isEqualTo(25);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
  void should_rejectNamingTheField_when_notPositive(int value) {
    assertThatThrownBy(() -> new PokemonId(value))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("id"));
  }
}
