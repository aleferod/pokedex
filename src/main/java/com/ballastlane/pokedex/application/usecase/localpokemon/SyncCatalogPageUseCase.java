package com.ballastlane.pokedex.application.usecase.localpokemon;

import com.ballastlane.pokedex.application.port.in.localpokemon.BulkSyncResult;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncCatalogPage;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncResult;
import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Syncs the first catalog entries one by one, in catalog order, by composing the catalog and sync
 * use cases. Not atomic on purpose: entries synced before a failure stay synced (each is valid on
 * its own), and the failure is reported to the caller.
 */
public class SyncCatalogPageUseCase implements SyncCatalogPage {

  private final ListPokemon listPokemon;
  private final SyncPokemon syncPokemon;

  public SyncCatalogPageUseCase(ListPokemon listPokemon, SyncPokemon syncPokemon) {
    this.listPokemon = listPokemon;
    this.syncPokemon = syncPokemon;
  }

  @Override
  public BulkSyncResult syncFirst(int limit, String actor) {
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new InvalidFieldException("limit", "must be between 1 and " + MAX_LIMIT);
    }
    List<BulkSyncResult.Entry> results = new ArrayList<>();
    for (PokemonSummary entry : listPokemon.list(new PageRequest(0, limit)).content()) {
      SyncResult result = syncPokemon.sync(new PokemonIdentifier.ById(entry.id()), actor);
      results.add(new BulkSyncResult.Entry(entry.id(), entry.name(), result.created()));
    }
    return new BulkSyncResult(results);
  }
}
