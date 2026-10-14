package com.ballastlane.pokedex.domain.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InternalTagsTest {

  @Test
  void should_trimLowercaseAndDeduplicateKeepingOrder_when_built() {
    InternalTags tags = InternalTags.of(List.of(" Starter-Line ", "electric", "ELECTRIC", "kanto"));

    assertThat(tags.tags()).containsExactly("starter-line", "electric", "kanto");
  }

  @Test
  void should_beEmpty_when_givenNull() {
    assertThat(InternalTags.of(null).tags()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"", "  ", "-starter", "with space", "emoji⚡", "abcdefghijklmnopqrstuvwxyz12345"})
  void should_rejectNamingTheField_when_aTagBreaksThePattern(String tag) {
    assertThatThrownBy(() -> InternalTags.of(List.of(tag)))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> assertThat(error.field()).isEqualTo("internalTags"));
  }

  @Test
  void should_reject_when_moreThanTwentyDistinctTags() {
    List<String> tags = IntStream.range(0, 21).mapToObj(i -> "tag-" + i).toList();

    assertThatThrownBy(() -> InternalTags.of(tags))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> assertThat(error.reason()).contains("at most 20"));
  }

  @Test
  void should_answerMembership_when_asked() {
    assertThat(InternalTags.of(List.of("legendary")).contains("LEGENDARY")).isTrue();
    assertThat(InternalTags.of(List.of("legendary")).contains("starter")).isFalse();
  }
}
