package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import com.ballastlane.pokedex.adapter.in.web.pokemon.PokemonDetailResponse.BaseStatsResponse;
import com.ballastlane.pokedex.application.port.in.localpokemon.BulkSyncResult;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.UpstreamData;

/** DTO + Mapper: local replica domain objects to the API's JSON shapes. */
final class LocalPokemonWebMapper {

  private LocalPokemonWebMapper() {}

  static LocalPokemonResponse toResponse(LocalPokemon pokemon) {
    UpstreamData upstream = pokemon.upstream();
    return new LocalPokemonResponse(
        pokemon.id().value(),
        pokemon.pokeapiId().value(),
        upstream.name(),
        upstream.spriteUrl(),
        upstream.imageUrl(),
        upstream.category(),
        upstream.types(),
        upstream.height().metres(),
        upstream.weight().kilograms(),
        upstream.abilities(),
        new BaseStatsResponse(
            upstream.stats().hp(),
            upstream.stats().attack(),
            upstream.stats().defense(),
            upstream.stats().specialAttack(),
            upstream.stats().specialDefense(),
            upstream.stats().speed()),
        pokemon.proprietary().localizedNames().names(),
        pokemon.proprietary().region(),
        pokemon.proprietary().habitatNotes(),
        pokemon.proprietary().internalTags().tags(),
        pokemon.proprietary().notes(),
        pokemon.audit().createdAt(),
        pokemon.audit().updatedAt(),
        pokemon.audit().syncedAt(),
        pokemon.audit().createdBy(),
        version(pokemon));
  }

  static BulkSyncResponse toResponse(BulkSyncResult result) {
    return new BulkSyncResponse(
        result.results().stream()
            .map(
                entry ->
                    new BulkSyncResponse.Entry(
                        entry.pokeapiId().value(),
                        entry.name(),
                        entry.created() ? "created" : "updated"))
            .toList(),
        result.created(),
        result.updated());
  }

  /** Strong ETag value for a stored record: its optimistic-locking version. */
  static String etag(LocalPokemon pokemon) {
    return "\"" + version(pokemon) + "\"";
  }

  private static long version(LocalPokemon pokemon) {
    return pokemon.version() == null ? 0L : pokemon.version();
  }
}
