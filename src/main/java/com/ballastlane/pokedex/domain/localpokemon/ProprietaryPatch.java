package com.ballastlane.pokedex.domain.localpokemon;

import java.util.List;
import java.util.Objects;

/**
 * A JSON Merge Patch (RFC 7396) of the proprietary data. Applying it yields raw values that go
 * through {@link ProprietaryData#of}, so PATCH and PUT share the same rules and report every
 * violation together. A missing component means "keep".
 */
public record ProprietaryPatch(
    LocalizedNamesPatch localizedNames,
    FieldPatch<String> region,
    FieldPatch<String> habitatNotes,
    FieldPatch<List<String>> internalTags,
    FieldPatch<String> notes) {

  public ProprietaryPatch {
    localizedNames = Objects.requireNonNullElse(localizedNames, LocalizedNamesPatch.keep());
    region = Objects.requireNonNullElse(region, FieldPatch.keep());
    habitatNotes = Objects.requireNonNullElse(habitatNotes, FieldPatch.keep());
    internalTags = Objects.requireNonNullElse(internalTags, FieldPatch.keep());
    notes = Objects.requireNonNullElse(notes, FieldPatch.keep());
  }

  /**
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldsException listing every rule the
   *     patched values break
   */
  public ProprietaryData applyTo(ProprietaryData current) {
    return ProprietaryData.of(
        localizedNames.applyTo(current.localizedNames().names()),
        region.resolve(current.region()),
        habitatNotes.resolve(current.habitatNotes()),
        internalTags.resolve(current.internalTags().tags()),
        notes.resolve(current.notes()));
  }
}
