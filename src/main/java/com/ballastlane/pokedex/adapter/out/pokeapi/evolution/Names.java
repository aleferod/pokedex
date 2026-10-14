package com.ballastlane.pokedex.adapter.out.pokeapi.evolution;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.NamedResourceDto;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Turns PokeAPI slugs into display text: {@code water-stone} -> {@code Water Stone}. */
final class Names {

  private Names() {}

  static String humanize(String slug) {
    return Arrays.stream(slug.split("-"))
        .filter(word -> !word.isEmpty())
        .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
        .collect(Collectors.joining(" "));
  }

  static String humanize(NamedResourceDto resource) {
    return humanize(resource.name());
  }

  static boolean present(NamedResourceDto resource) {
    return resource != null && resource.name() != null && !resource.name().isBlank();
  }
}
