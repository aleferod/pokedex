package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Value object: the fields the business owns and PokeAPI knows nothing about. Text is trimmed and
 * blank text becomes {@code null}; lengths are capped. Synchronization never touches this data.
 */
public record ProprietaryData(
    LocalizedNames localizedNames,
    String region,
    String habitatNotes,
    InternalTags internalTags,
    String notes) {

  public static final int MAX_REGION = 50;
  public static final int MAX_HABITAT_NOTES = 500;
  public static final int MAX_NOTES = 2000;

  public ProprietaryData {
    localizedNames = localizedNames == null ? LocalizedNames.empty() : localizedNames;
    internalTags = internalTags == null ? InternalTags.empty() : internalTags;
    region = text("region", region, MAX_REGION);
    habitatNotes = text("habitatNotes", habitatNotes, MAX_HABITAT_NOTES);
    notes = text("notes", notes, MAX_NOTES);
  }

  /**
   * Builds from untrusted input, checking every field before failing (Notification pattern), so a
   * client sees all of its mistakes in one answer. The canonical constructor fails fast instead,
   * which suits trusted callers.
   *
   * @throws InvalidFieldsException listing one violation per offending field
   */
  public static ProprietaryData of(
      Map<String, String> localizedNames,
      String region,
      String habitatNotes,
      Collection<String> internalTags,
      String notes) {
    List<InvalidFieldException> violations = new ArrayList<>();
    LocalizedNames names = collect(violations, () -> LocalizedNames.of(localizedNames));
    String validRegion = collect(violations, () -> text("region", region, MAX_REGION));
    String validHabitat =
        collect(violations, () -> text("habitatNotes", habitatNotes, MAX_HABITAT_NOTES));
    InternalTags tags = collect(violations, () -> InternalTags.of(internalTags));
    String validNotes = collect(violations, () -> text("notes", notes, MAX_NOTES));
    if (!violations.isEmpty()) {
      throw new InvalidFieldsException(violations);
    }
    return new ProprietaryData(names, validRegion, validHabitat, tags, validNotes);
  }

  public static ProprietaryData empty() {
    return new ProprietaryData(null, null, null, null, null);
  }

  private static <T> T collect(List<InvalidFieldException> violations, Supplier<T> part) {
    try {
      return part.get();
    } catch (InvalidFieldException e) {
      violations.add(e);
      return null;
    }
  }

  private static String text(String field, String value, int max) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.codePointCount(0, trimmed.length()) > max) {
      throw new InvalidFieldException(field, "must be at most " + max + " characters");
    }
    return trimmed;
  }
}
