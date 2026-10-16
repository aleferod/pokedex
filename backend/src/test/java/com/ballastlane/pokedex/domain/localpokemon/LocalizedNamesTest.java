package com.ballastlane.pokedex.domain.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocalizedNamesTest {

  @Test
  void should_trimNamesAndKeepLocaleOrder_when_valid() {
    Map<String, String> names = new LinkedHashMap<>();
    names.put("pt-BR", "  Pikachu  ");
    names.put("ja", "ピカチュウ");
    names.put("zh-Hant-TW", "皮卡丘");

    LocalizedNames localized = LocalizedNames.of(names);

    assertThat(localized.names())
        .containsExactly(
            Map.entry("ja", "ピカチュウ"),
            Map.entry("pt-BR", "Pikachu"),
            Map.entry("zh-Hant-TW", "皮卡丘"));
  }

  @Test
  void should_beEmpty_when_givenNull() {
    assertThat(LocalizedNames.of(null).names()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"portuguese", "pt_BR", "P", "", "pt-BR-x-very-long-extension"})
  void should_rejectNamingTheField_when_localeIsInvalid(String locale) {
    assertThatThrownBy(() -> LocalizedNames.of(Map.of(locale, "Pikachu")))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> assertThat(error.field()).isEqualTo("localizedNames"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "123456789012345678901234567890123456789012345678901"})
  void should_rejectNamingTheLocale_when_nameIsBlankOrTooLong(String name) {
    assertThatThrownBy(() -> LocalizedNames.of(Map.of("pt-BR", name)))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> assertThat(error.field()).isEqualTo("localizedNames.pt-BR"));
  }

  @Test
  void should_reject_when_moreThanTenLocales() {
    Map<String, String> eleven =
        IntStream.range(0, 11)
            .boxed()
            .collect(Collectors.toMap(i -> "l" + (char) ('a' + i), i -> "Name " + i));

    assertThatThrownBy(() -> LocalizedNames.of(eleven))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> assertThat(error.reason()).contains("at most 10"));
  }

  @Test
  void should_beImmutable_when_built() {
    assertThatThrownBy(() -> LocalizedNames.of(Map.of("en", "Pikachu")).names().put("fr", "x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
