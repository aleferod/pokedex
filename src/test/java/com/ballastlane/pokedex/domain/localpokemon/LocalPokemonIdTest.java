package com.ballastlane.pokedex.domain.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LocalPokemonIdTest {

  @Test
  void should_parseUuid_when_valid() {
    UUID uuid = UUID.randomUUID();
    assertThat(LocalPokemonId.parse(uuid.toString())).isEqualTo(new LocalPokemonId(uuid));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"not-a-uuid", "123"})
  void should_rejectNamingId_when_notAUuid(String value) {
    assertThatThrownBy(() -> LocalPokemonId.parse(value))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("id"));
  }

  @Test
  void should_reject_when_null() {
    assertThatThrownBy(() -> new LocalPokemonId(null)).isInstanceOf(InvalidFieldException.class);
  }
}
