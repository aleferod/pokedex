package com.ballastlane.pokedex.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PokemonNameQueryTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  void should_meanNoFilter_when_nameIsBlank(String raw) {
    assertThat(PokemonNameQuery.parse(raw)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "pika, pika",
    "'  PIKA ', pika",
    "'mr mime', mr-mime",
    "'Mr   Mime', mr-mime",
    "porygon2, porygon2",
    "nidoran-f, nidoran-f",
    "123, 123"
  })
  void should_normaliseLikePokeApiNames_when_nameIsValid(String raw, String value) {
    assertThat(PokemonNameQuery.parse(raw))
        .hasValueSatisfying(query -> assertThat(query.value()).isEqualTo(value));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "pi",
        " p ",
        "pika!",
        "pika.chu",
        "<script>",
        "pikächu",
        "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy"
      })
  void should_rejectNamingTheField_when_nameIsTooShortTooLongOrHasOtherCharacters(String raw) {
    assertThatThrownBy(() -> PokemonNameQuery.parse(raw))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> {
              assertThat(error.field()).isEqualTo("name");
              assertThat(error.reason())
                  .isEqualTo("must be 3 to 50 letters, digits, hyphens or spaces");
            });
  }

  @Test
  void should_acceptExactlyTheLimits_when_nameIs3Or50Characters() {
    assertThat(PokemonNameQuery.parse("abc")).isPresent();
    assertThat(PokemonNameQuery.parse("a".repeat(50))).isPresent();
  }

  @Test
  void should_matchNamesContainingTheQuery_when_compared() {
    PokemonNameQuery pika = PokemonNameQuery.parse("Pika").orElseThrow();

    assertThat(pika.matches("pikachu")).isTrue();
    assertThat(pika.matches("pikachu-rock-star")).isTrue();
    assertThat(pika.matches("raichu")).isFalse();
    assertThat(PokemonNameQuery.parse("chu").orElseThrow().matches("pikachu")).isTrue();
    assertThat(PokemonNameQuery.parse("mr mime").orElseThrow().matches("mr-mime-galar")).isTrue();
    assertThat(PokemonNameQuery.parse("mr mime").orElseThrow().matches("mime-jr")).isFalse();
  }
}
