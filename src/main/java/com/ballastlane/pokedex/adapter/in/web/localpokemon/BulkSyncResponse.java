package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import java.util.List;

/** Result of {@code POST /api/v1/local-pokemon/sync?limit=N}. */
public record BulkSyncResponse(List<Entry> results, long created, long updated) {

  /** One synced catalog entry; {@code status} is {@code created} or {@code updated}. */
  public record Entry(int pokeapiId, String name, String status) {}
}
