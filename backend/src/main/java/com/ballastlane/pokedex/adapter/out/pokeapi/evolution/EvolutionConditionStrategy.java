package com.ballastlane.pokedex.adapter.out.pokeapi.evolution;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionDetailDto;

/**
 * Strategy: turns one PokeAPI evolution method into a sentence a player understands. One
 * implementation per PokeAPI trigger ({@code level-up}, {@code use-item}, {@code trade}, ...),
 * chosen by {@link EvolutionConditions}.
 */
public interface EvolutionConditionStrategy {

  String describe(EvolutionDetailDto detail);
}
