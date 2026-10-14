package com.ballastlane.pokedex.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WeightTest {

  @ParameterizedTest
  @CsvSource({"69, 6.9", "0, 0.0", "1, 0.1", "9999, 999.9", "60, 6.0"})
  void should_convertHectogramsToKilogramsExactly_when_asked(int hectograms, String kilograms) {
    assertThat(Weight.ofHectograms(hectograms).kilograms()).isEqualTo(new BigDecimal(kilograms));
  }

  @Test
  void should_rejectNamingTheField_when_negative() {
    assertThatThrownBy(() -> Weight.ofHectograms(-1))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("weight"));
  }
}
