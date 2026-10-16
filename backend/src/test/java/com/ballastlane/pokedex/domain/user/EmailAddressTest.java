package com.ballastlane.pokedex.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {

  @Test
  void should_trimAndLowercase_when_created() {
    assertThat(new EmailAddress("  Ash@Pallet.Town ").value()).isEqualTo("ash@pallet.town");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ash@pallet.town", "a.b+tag@sub.domain.io", "x_y-z@kanto.co.jp"})
  void should_accept_when_addressIsValid(String candidate) {
    assertThat(new EmailAddress(candidate).value()).isEqualTo(candidate);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not-an-email",
        "a@b",
        "@pallet.town",
        "ash@",
        "ash@@pallet.town",
        "ash pallet@kanto.io",
        "ash@pallet..town",
        "ash@.pallet.town"
      })
  void should_rejectNamingTheField_when_addressIsMalformed(String candidate) {
    assertThatThrownBy(() -> new EmailAddress(candidate))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("email"));
  }

  @Test
  void should_reject_when_addressIsLongerThan254Characters() {
    String tooLong = "a".repeat(64) + "@" + "b".repeat(186) + ".com";
    assertThat(tooLong).hasSize(255);
    assertThatThrownBy(() -> new EmailAddress(tooLong)).isInstanceOf(InvalidFieldException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  void should_rejectAsRequired_when_addressIsMissing(String candidate) {
    assertThatThrownBy(() -> new EmailAddress(candidate))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> assertThat(error.reason()).isEqualTo("is required"));
  }
}
