package com.ballastlane.pokedex.application.port.in.localpokemon;

/** Use case (US03, bulk): sync the first {@code limit} catalog entries (1 to 20). */
public interface SyncCatalogPage {

  int MAX_LIMIT = 20;

  /**
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldException limit outside 1..20
   */
  BulkSyncResult syncFirst(int limit, String actor);
}
