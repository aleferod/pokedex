package com.ballastlane.pokedex.domain.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldsException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProprietaryDataTest {

  @Test
  void should_haveNothingSet_when_empty() {
    ProprietaryData empty = ProprietaryData.empty();

    assertThat(empty.localizedNames().names()).isEmpty();
    assertThat(empty.internalTags().tags()).isEmpty();
    assertThat(empty.region()).isNull();
    assertThat(empty.habitatNotes()).isNull();
    assertThat(empty.notes()).isNull();
  }

  @Test
  void should_trimTextAndTurnBlankIntoNull_when_built() {
    ProprietaryData data =
        new ProprietaryData(
            LocalizedNames.empty(), "  Kanto ", "   ", InternalTags.empty(), "\n Loves ketchup \t");

    assertThat(data.region()).isEqualTo("Kanto");
    assertThat(data.habitatNotes()).isNull();
    assertThat(data.notes()).isEqualTo("Loves ketchup");
  }

  @Test
  void should_defaultMissingCollectionsToEmpty_when_null() {
    ProprietaryData data = new ProprietaryData(null, null, null, null, null);

    assertThat(data.localizedNames()).isEqualTo(LocalizedNames.empty());
    assertThat(data.internalTags()).isEqualTo(InternalTags.empty());
  }

  @Test
  void should_rejectNamingTheField_when_textIsTooLong() {
    assertThatThrownBy(() -> withRegion("x".repeat(51))).satisfies(field("region"));
    assertThatThrownBy(() -> new ProprietaryData(null, null, "x".repeat(501), null, null))
        .satisfies(field("habitatNotes"));
    assertThatThrownBy(() -> new ProprietaryData(null, null, null, null, "x".repeat(2001)))
        .satisfies(field("notes"));
  }

  @Test
  void should_acceptTheLimits_when_textIsExactlyAtTheLimit() {
    assertThat(withRegion("x".repeat(50)).region()).hasSize(50);
    assertThat(new ProprietaryData(null, null, "x".repeat(500), null, "y".repeat(2000)).notes())
        .hasSize(2000);
  }

  @Test
  void should_reportEveryViolation_when_builtFromRawInput() {
    assertThatThrownBy(
            () ->
                ProprietaryData.of(
                    Map.of("not a locale", "x"),
                    "x".repeat(51),
                    null,
                    List.of("Bad Tag!"),
                    "y".repeat(2001)))
        .isInstanceOfSatisfying(
            InvalidFieldsException.class,
            error ->
                assertThat(error.violations())
                    .extracting(InvalidFieldException::field)
                    .containsExactlyInAnyOrder(
                        "localizedNames", "region", "internalTags", "notes"));
  }

  @Test
  void should_equalTheValidatedConstruction_when_rawInputIsValid() {
    assertThat(
            ProprietaryData.of(
                Map.of("pt-BR", "Pikachu"), " Kanto ", null, List.of("Starter"), null))
        .isEqualTo(
            new ProprietaryData(
                LocalizedNames.of(Map.of("pt-BR", "Pikachu")),
                "Kanto",
                null,
                InternalTags.of(List.of("starter")),
                null));
  }

  @Test
  void should_reportTheTagField_when_aTagIsNull() {
    assertThatThrownBy(() -> ProprietaryData.of(null, null, null, Arrays.asList("ok", null), null))
        .isInstanceOfSatisfying(
            InvalidFieldsException.class,
            error ->
                assertThat(error.violations())
                    .extracting(InvalidFieldException::field)
                    .containsExactly("internalTags"));
  }

  private static ProprietaryData withRegion(String region) {
    return new ProprietaryData(null, region, null, null, null);
  }

  private static org.assertj.core.api.ThrowingConsumer<Throwable> field(String name) {
    return error ->
        assertThat(error)
            .isInstanceOfSatisfying(
                InvalidFieldException.class,
                invalid -> assertThat(invalid.field()).isEqualTo(name));
  }
}
