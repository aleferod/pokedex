package com.ballastlane.pokedex.adapter.in.web.pokemon;

import com.ballastlane.pokedex.adapter.in.web.PageResponse;
import com.ballastlane.pokedex.domain.pokemon.EvolutionStage;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;

/** DTO + Mapper: catalog domain objects to the catalog API's JSON shapes. */
final class PokemonWebMapper {

  private PokemonWebMapper() {}

  static PageResponse<PokemonSummaryResponse> toResponse(Page<PokemonSummary> page) {
    return PageResponse.of(page, PokemonWebMapper::toResponse);
  }

  static PokemonSummaryResponse toResponse(PokemonSummary summary) {
    return new PokemonSummaryResponse(
        summary.id().value(),
        summary.name(),
        summary.spriteUrl(),
        summary.category(),
        summary.weight().kilograms(),
        summary.abilities());
  }

  static PokemonDetailResponse toResponse(PokemonDetail detail) {
    return new PokemonDetailResponse(
        detail.id().value(),
        detail.name(),
        detail.imageUrl(),
        detail.category(),
        detail.description(),
        detail.types(),
        detail.height().metres(),
        detail.weight().kilograms(),
        detail.abilities(),
        new PokemonDetailResponse.BaseStatsResponse(
            detail.stats().hp(),
            detail.stats().attack(),
            detail.stats().defense(),
            detail.stats().specialAttack(),
            detail.stats().specialDefense(),
            detail.stats().speed()),
        toResponse(detail.evolutionChain()));
  }

  static PokemonDetailResponse.EvolutionStageResponse toResponse(EvolutionStage stage) {
    return new PokemonDetailResponse.EvolutionStageResponse(
        stage.id().value(),
        stage.name(),
        stage.condition(),
        stage.evolvesTo().stream().map(PokemonWebMapper::toResponse).toList());
  }
}
