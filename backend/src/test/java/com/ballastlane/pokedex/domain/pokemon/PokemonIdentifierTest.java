package com.ballastlane.pokedex.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PokemonIdentifierTest {

  @ParameterizedTest
  @CsvSource({"25, 25", "' 0025 ', 25", "1, 1", "99999, 99999", "10001, 10001"})
  void should_parseANumber_when_identifierIsDigitsOnly(String raw, int id) {
    assertThat(PokemonIdentifier.parse(raw))
        .isEqualTo(new PokemonIdentifier.ById(new PokemonId(id)));
  }

  @ParameterizedTest
  @CsvSource({
    "Pikachu, pikachu",
    "'  MR-MIME ', mr-mime",
    "porygon2, porygon2",
    "nidoran-f, nidoran-f",
    "a, a"
  })
  void should_parseALowerCaseName_when_identifierIsAName(String raw, String name) {
    assertThat(PokemonIdentifier.parse(raw)).isEqualTo(new PokemonIdentifier.ByName(name));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "   ",
        "0",
        "100000",
        "-1",
        "pika chu",
        "<script>",
        "pikachu!",
        "2pac",
        "mr.mime",
        "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy"
      })
  void should_rejectNamingTheField_when_identifierIsMalformed(String raw) {
    assertThatThrownBy(() -> PokemonIdentifier.parse(raw))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("idOrName"));
  }

  @Test
  void should_exposeTheUpstreamPathSegment_when_asked() {
    assertThat(PokemonIdentifier.parse("25").value()).isEqualTo("25");
    assertThat(PokemonIdentifier.parse("Pikachu").value()).isEqualTo("pikachu");
  }
}
