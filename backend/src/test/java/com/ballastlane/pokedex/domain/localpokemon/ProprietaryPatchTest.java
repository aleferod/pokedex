package com.ballastlane.pokedex.domain.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldsException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProprietaryPatchTest {

  private static final ProprietaryData CURRENT =
      new ProprietaryData(
          LocalizedNames.of(Map.of("pt-BR", "Pikachu", "ja", "ピカチュウ")),
          "Kanto",
          "Viridian Forest",
          InternalTags.of(List.of("starter-line")),
          "old");

  @Test
  void should_changeNothing_when_everyFieldIsKept() {
    assertThat(patch(LocalizedNamesPatch.keep(), keep(), keep(), keep(), keep()).applyTo(CURRENT))
        .isEqualTo(CURRENT);
  }

  @Test
  void should_replaceOnlyTheGivenField_when_oneFieldIsSet() {
    ProprietaryData patched =
        patch(LocalizedNamesPatch.keep(), keep(), keep(), keep(), FieldPatch.set("Loves ketchup"))
            .applyTo(CURRENT);

    assertThat(patched.notes()).isEqualTo("Loves ketchup");
    assertThat(patched.region()).isEqualTo("Kanto");
    assertThat(patched.localizedNames()).isEqualTo(CURRENT.localizedNames());
  }

  @Test
  void should_removeTheValue_when_aFieldIsCleared() {
    ProprietaryData patched =
        patch(LocalizedNamesPatch.keep(), FieldPatch.clear(), keep(), FieldPatch.clear(), keep())
            .applyTo(CURRENT);

    assertThat(patched.region()).isNull();
    assertThat(patched.internalTags().tags()).isEmpty();
    assertThat(patched.habitatNotes()).isEqualTo("Viridian Forest");
  }

  @Test
  void should_mergeLocalizedNamesPerLocale_when_namesArePatched() {
    ProprietaryData patched =
        patch(
                LocalizedNamesPatch.merge(Map.of("fr", "Pikachu"), Set.of("ja")),
                keep(),
                keep(),
                keep(),
                keep())
            .applyTo(CURRENT);

    assertThat(patched.localizedNames().names())
        .containsExactly(Map.entry("fr", "Pikachu"), Map.entry("pt-BR", "Pikachu"));
  }

  @Test
  void should_dropEveryName_when_localizedNamesAreCleared() {
    ProprietaryData patched =
        patch(LocalizedNamesPatch.clear(), keep(), keep(), keep(), keep()).applyTo(CURRENT);

    assertThat(patched.localizedNames().names()).isEmpty();
  }

  @Test
  void should_normalizeTheNewValues_when_set() {
    ProprietaryData patched =
        patch(
                LocalizedNamesPatch.keep(),
                FieldPatch.set("  Johto  "),
                keep(),
                FieldPatch.set(List.of("Starter", "starter", " KANTO ")),
                keep())
            .applyTo(CURRENT);

    assertThat(patched.region()).isEqualTo("Johto");
    assertThat(patched.internalTags().tags()).containsExactly("starter", "kanto");
  }

  @Test
  void should_reportEveryViolation_when_severalPatchedFieldsAreInvalid() {
    ProprietaryPatch invalid =
        patch(
            LocalizedNamesPatch.keep(),
            FieldPatch.set("x".repeat(51)),
            keep(),
            FieldPatch.set(List.of("Bad Tag!")),
            keep());

    assertThatThrownBy(() -> invalid.applyTo(CURRENT))
        .isInstanceOfSatisfying(
            InvalidFieldsException.class,
            error ->
                assertThat(error.violations())
                    .extracting(InvalidFieldException::field)
                    .containsExactlyInAnyOrder("region", "internalTags"));
  }

  private static <T> FieldPatch<T> keep() {
    return FieldPatch.keep();
  }

  private static ProprietaryPatch patch(
      LocalizedNamesPatch names,
      FieldPatch<String> region,
      FieldPatch<String> habitat,
      FieldPatch<List<String>> tags,
      FieldPatch<String> notes) {
    return new ProprietaryPatch(names, region, habitat, tags, notes);
  }
}
