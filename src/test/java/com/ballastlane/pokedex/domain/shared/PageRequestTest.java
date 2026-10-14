package com.ballastlane.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PageRequestTest {

  @ParameterizedTest
  @CsvSource({"0, 20, 0", "2, 20, 40", "500, 20, 10000", "0, 1, 0", "3, 100, 300"})
  void should_computeTheOffset_when_valid(int page, int size, long offset) {
    assertThat(new PageRequest(page, size).offset()).isEqualTo(offset);
  }

  @Test
  void should_notOverflow_when_pageAndSizeAreLarge() {
    assertThat(new PageRequest(Integer.MAX_VALUE, 100).offset())
        .isEqualTo(100L * Integer.MAX_VALUE);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, Integer.MIN_VALUE})
  void should_rejectNamingPage_when_negative(int page) {
    assertThatThrownBy(() -> new PageRequest(page, 20))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("page"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -5, 101})
  void should_rejectNamingSize_when_outsideOneToHundred(int size) {
    assertThatThrownBy(() -> new PageRequest(0, size))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("size"));
  }
}
