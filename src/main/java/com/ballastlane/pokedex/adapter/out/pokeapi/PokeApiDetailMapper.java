package com.ballastlane.pokedex.adapter.out.pokeapi;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionChainDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonSpeciesDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.evolution.EvolutionConditions;
import com.ballastlane.pokedex.domain.pokemon.BaseStats;
import com.ballastlane.pokedex.domain.pokemon.EvolutionStage;
import com.ballastlane.pokedex.domain.pokemon.Height;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.Weight;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Anti-corruption layer for the detail view: combines {@code /pokemon}, {@code /pokemon-species}
 * and {@code /evolution-chain} into a {@link PokemonDetail}. Evolution conditions are delegated to
 * the {@link EvolutionConditions} strategies.
 */
final class PokeApiDetailMapper {

  private final EvolutionConditions conditions;

  PokeApiDetailMapper(EvolutionConditions conditions) {
    this.conditions = conditions;
  }

  PokemonDetail toDetail(PokemonDto pokemon, PokemonSpeciesDto species, EvolutionChainDto chain) {
    PokemonSummaryParts summary = PokemonSummaryParts.of(pokemon, species);
    return new PokemonDetail(
        new PokemonId(pokemon.id()),
        pokemon.name(),
        officialArtwork(pokemon.sprites()),
        summary.category(),
        newestEnglishDescription(species),
        typeNames(pokemon.types()),
        Height.ofDecimetres(pokemon.height()),
        Weight.ofHectograms(pokemon.weight()),
        summary.abilities(),
        baseStats(pokemon.stats()),
        stage(chain.chain()));
  }

  private EvolutionStage stage(EvolutionChainDto.ChainLink link) {
    return new EvolutionStage(
        new PokemonId(PokeApiMapper.idFromUrl(link.species().url())),
        link.species().name(),
        conditions.describe(link.evolutionDetails()),
        link.evolvesTo().stream().map(this::stage).toList());
  }

  private static String officialArtwork(PokemonDto.Sprites sprites) {
    if (sprites == null || sprites.other() == null || sprites.other().officialArtwork() == null) {
      return null;
    }
    String url = sprites.other().officialArtwork().frontDefault();
    return url == null || url.isBlank() ? null : url;
  }

  /** PokeAPI lists flavor texts in game order, so the last English one is the newest. */
  private static String newestEnglishDescription(PokemonSpeciesDto species) {
    List<PokemonSpeciesDto.FlavorTextEntry> english =
        species.flavorTextEntries().stream()
            .filter(
                entry ->
                    entry.language() != null
                        && PokeApiMapper.ENGLISH.equals(entry.language().name()))
            .toList();
    return english.isEmpty() ? null : FlavorText.clean(english.getLast().flavorText());
  }

  private static List<String> typeNames(List<PokemonDto.TypeSlot> types) {
    return types.stream()
        .filter(slot -> slot.type() != null)
        .sorted(Comparator.comparingInt(PokemonDto.TypeSlot::slot))
        .map(slot -> slot.type().name())
        .toList();
  }

  private static BaseStats baseStats(List<PokemonDto.StatSlot> stats) {
    Map<String, Integer> byName =
        stats.stream()
            .filter(slot -> slot.stat() != null)
            .collect(
                Collectors.toMap(
                    slot -> slot.stat().name(), PokemonDto.StatSlot::baseStat, (a, b) -> a));
    return new BaseStats(
        byName.getOrDefault("hp", 0),
        byName.getOrDefault("attack", 0),
        byName.getOrDefault("defense", 0),
        byName.getOrDefault("special-attack", 0),
        byName.getOrDefault("special-defense", 0),
        byName.getOrDefault("speed", 0));
  }

  /** The parts shared with the catalog summary, mapped once by {@link PokeApiMapper}. */
  private record PokemonSummaryParts(String category, List<String> abilities) {
    static PokemonSummaryParts of(PokemonDto pokemon, PokemonSpeciesDto species) {
      var summary = PokeApiMapper.toSummary(pokemon, species);
      return new PokemonSummaryParts(summary.category(), summary.abilities());
    }
  }
}
