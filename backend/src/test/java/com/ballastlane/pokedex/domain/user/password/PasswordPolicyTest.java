package com.ballastlane.pokedex.domain.user.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

  private final PasswordPolicy policy = PasswordPolicy.standard();

  @ParameterizedTest
  @ValueSource(strings = {"Pikachu@25", "Admin@123", "Trainer@123", "Ab1!Ab1!"})
  void should_acceptPassword_when_everyRuleIsMet(String password) {
    assertThat(policy.violationsOf(password)).isEmpty();
  }

  @Test
  void should_reportEveryBrokenRuleAtOnce_when_passwordIsWeak() {
    assertThat(policy.violationsOf("password"))
        .containsExactly(
            "must contain an upper-case letter",
            "must contain a digit",
            "must contain a character that is neither a letter nor a digit");
  }

  @Test
  void should_reportMissingLowerCase_when_passwordIsAllUpperCase() {
    assertThat(policy.violationsOf("PASSWORD1!"))
        .containsExactly("must contain a lower-case letter");
  }

  @Test
  void should_reportLength_when_passwordIsTooShort() {
    assertThat(policy.violationsOf("Ab1@x")).containsExactly("must be 8 to 72 characters");
  }

  @Test
  void should_acceptPassword_when_lengthIsExactly72() {
    String password = "Aa1@" + "x".repeat(68);
    assertThat(password).hasSize(72);
    assertThat(policy.violationsOf(password)).isEmpty();
  }

  @Test
  void should_reportLength_when_passwordIs73Characters() {
    String password = "Aa1@" + "x".repeat(69);
    assertThat(policy.violationsOf(password)).containsExactly("must be 8 to 72 characters");
  }

  @Test
  void should_reportLength_when_multiByteCharactersExceed72Bytes() {
    // 40 characters but 77 bytes in UTF-8: BCrypt only reads the first 72 bytes.
    String password = "Ä".repeat(37) + "a1@";
    assertThat(policy.violationsOf(password)).contains("must be 8 to 72 characters");
  }

  @Test
  void should_reportRequired_when_passwordIsNull() {
    assertThat(policy.violationsOf(null)).containsExactly("is required");
  }

  @Test
  void should_throwWithAllViolations_when_checkingWeakPassword() {
    assertThatThrownBy(() -> policy.check("password"))
        .isInstanceOfSatisfying(
            WeakPasswordException.class, error -> assertThat(error.violations()).hasSize(3));
  }

  @Test
  void should_notThrow_when_checkingStrongPassword() {
    policy.check("Pikachu@25");
  }

  @Test
  void should_applyExactlyTheGivenRules_when_builtWithACustomChain() {
    // Given a chain extended with one more rule (open for extension, closed for modification)
    PasswordRule noPokemonNames =
        password ->
            password.toLowerCase().contains("pikachu")
                ? Optional.of("must not contain a Pokémon name")
                : Optional.empty();
    PasswordPolicy custom = new PasswordPolicy(List.of(noPokemonNames));

    // When / Then
    assertThat(custom.violationsOf("Pikachu@25"))
        .containsExactly("must not contain a Pokémon name");
  }
}
