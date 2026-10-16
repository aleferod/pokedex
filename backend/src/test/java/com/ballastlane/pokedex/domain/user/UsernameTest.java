package com.ballastlane.pokedex.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UsernameTest {

  @Test
  void should_trimAndLowercase_when_created() {
    assertThat(new Username("  Misty  ").value()).isEqualTo("misty");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ash", "ash.ketchum_01-x", "abcdefghijklmnopqrstuvwxyz0123"})
  void should_accept_when_usernameFollowsTheRules(String candidate) {
    assertThat(new Username(candidate).value()).isEqualTo(candidate);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"ab", "abcdefghijklmnopqrstuvwxyz01234", "a!", "ash ketchum", "ação", "<b>x</b>"})
  void should_rejectNamingTheField_when_usernameBreaksTheRules(String candidate) {
    assertThatThrownBy(() -> new Username(candidate))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> {
              assertThat(error.field()).isEqualTo("username");
              assertThat(error.reason()).contains("3 to 30");
            });
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void should_rejectAsRequired_when_usernameIsMissing(String candidate) {
    assertThatThrownBy(() -> new Username(candidate))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> assertThat(error.reason()).isEqualTo("is required"));
  }
}
