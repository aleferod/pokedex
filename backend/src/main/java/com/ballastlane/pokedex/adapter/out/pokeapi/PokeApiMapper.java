package com.ballastlane.pokedex.adapter.out.pokeapi;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonSpeciesDto;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.pokemon.Weight;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anti-corruption layer: the only place that knows PokeAPI's vocabulary (hectograms, genera by
 * language, ability slots, ids inside resource URLs) and translates it into domain objects.
 */
final class PokeApiMapper {

  static final String ENGLISH = "en";

  private static final Pattern TRAILING_ID = Pattern.compile("/(\\d+)/?$");

  private PokeApiMapper() {}

  static PokemonSummary toSummary(PokemonDto pokemon, PokemonSpeciesDto species) {
    return new PokemonSummary(
        new PokemonId(pokemon.id()),
        pokemon.name(),
        pokemon.sprites() == null ? null : blankToNull(pokemon.sprites().frontDefault()),
        englishGenus(species),
        Weight.ofHectograms(pokemon.weight()),
        abilityNames(pokemon.abilities()));
  }

  /** Id at the end of a resource URL such as {@code .../pokemon-species/25/}. */
  static int idFromUrl(String url) {
    Matcher matcher = TRAILING_ID.matcher(url == null ? "" : url);
    if (!matcher.find()) {
      throw new IllegalArgumentException("No resource id in URL: " + url);
    }
    return Integer.parseInt(matcher.group(1));
  }

  private static String englishGenus(PokemonSpeciesDto species) {
    return species.genera().stream()
        .filter(genus -> genus.language() != null && ENGLISH.equals(genus.language().name()))
        .map(PokemonSpeciesDto.Genus::genus)
        .map(PokeApiMapper::blankToNull)
        .filter(genus -> genus != null)
        .findFirst()
        .orElse(null);
  }

  private static List<String> abilityNames(List<PokemonDto.AbilitySlot> abilities) {
    return abilities.stream()
        .filter(slot -> slot.ability() != null && slot.ability().name() != null)
        .sorted(Comparator.comparingInt(PokemonDto.AbilitySlot::slot))
        .map(slot -> slot.ability().name())
        .toList();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
