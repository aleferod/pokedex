package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;

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

  public static ProprietaryData empty() {
    return new ProprietaryData(null, null, null, null, null);
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
