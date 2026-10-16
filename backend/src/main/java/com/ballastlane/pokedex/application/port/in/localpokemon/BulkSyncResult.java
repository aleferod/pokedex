package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import java.util.List;

/** Outcome of a bulk sync: one entry per synced Pokémon, in catalog order. */
public record BulkSyncResult(List<Entry> results) {

  public BulkSyncResult {
    results = List.copyOf(results);
  }

  public long created() {
    return results.stream().filter(Entry::created).count();
  }

  public long updated() {
    return results.size() - created();
  }

  /** One synced Pokémon. */
  public record Entry(PokemonId pokeapiId, String name, boolean created) {}
}
