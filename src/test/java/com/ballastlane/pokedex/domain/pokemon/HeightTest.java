package com.ballastlane.pokedex.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HeightTest {

  @ParameterizedTest
  @CsvSource({"7, 0.7", "0, 0.0", "20, 2.0", "145, 14.5"})
  void should_convertDecimetresToMetresExactly_when_asked(int decimetres, String metres) {
    assertThat(Height.ofDecimetres(decimetres).metres()).isEqualTo(new BigDecimal(metres));
  }

  @Test
  void should_rejectNamingTheField_when_negative() {
    assertThatThrownBy(() -> Height.ofDecimetres(-1))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("height"));
  }
}
