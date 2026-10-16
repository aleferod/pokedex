package com.ballastlane.pokedex.domain.localpokemon;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Merge patch of the localized names, which RFC 7396 merges per member: {@code put} adds or
 * replaces locales, {@code remove} drops them (members set to {@code null}), and {@code clearAll}
 * drops every locale ({@code "localizedNames": null}).
 */
public record LocalizedNamesPatch(boolean clearAll, Map<String, String> put, Set<String> remove) {

  public LocalizedNamesPatch {
    put = put == null ? Map.of() : Map.copyOf(put);
    remove = remove == null ? Set.of() : Set.copyOf(remove);
  }

  public static LocalizedNamesPatch keep() {
    return new LocalizedNamesPatch(false, Map.of(), Set.of());
  }

  public static LocalizedNamesPatch clear() {
    return new LocalizedNamesPatch(true, Map.of(), Set.of());
  }

  public static LocalizedNamesPatch merge(Map<String, String> put, Set<String> remove) {
    return new LocalizedNamesPatch(false, put, remove);
  }

  /** The raw names after the patch; validation happens when they become {@link LocalizedNames}. */
  public Map<String, String> applyTo(Map<String, String> current) {
    Map<String, String> result = new TreeMap<>();
    if (!clearAll && current != null) {
      result.putAll(current);
    }
    remove.forEach(result::remove);
    result.putAll(put);
    return result;
  }
}
