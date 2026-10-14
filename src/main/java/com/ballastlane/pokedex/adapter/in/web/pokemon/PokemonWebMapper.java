package com.ballastlane.pokedex.adapter.in.web.pokemon;

import com.ballastlane.pokedex.adapter.in.web.PageResponse;
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
}
