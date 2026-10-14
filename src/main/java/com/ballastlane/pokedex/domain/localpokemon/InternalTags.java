package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Value object: internal classification tags (proprietary data). Trimmed, lower-cased and
 * de-duplicated in first-seen order; each 1 to 30 characters of letters, digits and hyphens,
 * starting with a letter or digit; at most 20 tags.
 */
public record InternalTags(List<String> tags) {

  public static final int MAX_TAGS = 20;
  private static final Pattern TAG = Pattern.compile("[a-z0-9][a-z0-9-]{0,29}");

  public InternalTags {
    Set<String> normalized = new LinkedHashSet<>();
    if (tags != null) {
      for (String tag : tags) {
        normalized.add(normalize(tag));
      }
    }
    if (normalized.size() > MAX_TAGS) {
      throw new InvalidFieldException("internalTags", "must have at most " + MAX_TAGS + " tags");
    }
    tags = List.copyOf(normalized);
  }

  public static InternalTags empty() {
    return new InternalTags(List.of());
  }

  public static InternalTags of(Collection<String> tags) {
    return new InternalTags(tags == null ? null : List.copyOf(tags));
  }

  public boolean contains(String tag) {
    return tag != null && tags.contains(tag.trim().toLowerCase(Locale.ROOT));
  }

  private static String normalize(String tag) {
    String value = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
    if (!TAG.matcher(value).matches()) {
      throw new InvalidFieldException(
          "internalTags",
          "'"
              + tag
              + "' must be 1 to 30 lower-case letters, digits or hyphens, starting with a letter"
              + " or digit");
    }
    return value;
  }
}
