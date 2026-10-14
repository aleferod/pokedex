package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import com.ballastlane.pokedex.application.port.in.localpokemon.GetLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncCatalogPage;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncResult;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local replica (US03): import or refresh Pokémon from PokeAPI and read them back. Authenticated
 * only; the caller's username is recorded as {@code createdBy} on first import. Every record
 * response carries an {@code ETag} with the record's version (used by the editing API).
 */
@RestController
@RequestMapping(LocalPokemonController.BASE_PATH)
@Tag(name = "Local Pokémon", description = "Locally owned copies with proprietary fields")
@SecurityRequirement(name = "bearer-jwt")
public class LocalPokemonController {

  public static final String BASE_PATH = "/api/v1/local-pokemon";

  private final SyncPokemon syncPokemon;
  private final SyncCatalogPage syncCatalogPage;
  private final GetLocalPokemon getLocalPokemon;

  public LocalPokemonController(
      SyncPokemon syncPokemon, SyncCatalogPage syncCatalogPage, GetLocalPokemon getLocalPokemon) {
    this.syncPokemon = syncPokemon;
    this.syncCatalogPage = syncCatalogPage;
    this.getLocalPokemon = getLocalPokemon;
  }

  @PostMapping("/sync/{idOrName}")
  @Operation(summary = "Import a Pokémon from PokeAPI (201) or refresh its upstream fields (200)")
  @ApiResponse(responseCode = "201", description = "Imported; Location points to the new record")
  @ApiResponse(responseCode = "200", description = "Re-synced; proprietary fields untouched")
  @ApiResponse(responseCode = "404", description = "Unknown Pokémon")
  @ApiResponse(responseCode = "503", description = "PokeAPI unavailable; nothing changed")
  public ResponseEntity<LocalPokemonResponse> sync(
      @Parameter(example = "pikachu") @PathVariable String idOrName, Authentication caller) {
    SyncResult result = syncPokemon.sync(PokemonIdentifier.parse(idOrName), caller.getName());
    LocalPokemon pokemon = result.pokemon();
    ResponseEntity.BodyBuilder response =
        result.created()
            ? ResponseEntity.created(URI.create(BASE_PATH + "/" + pokemon.id().value()))
            : ResponseEntity.ok();
    return response
        .eTag(LocalPokemonWebMapper.etag(pokemon))
        .body(LocalPokemonWebMapper.toResponse(pokemon));
  }

  @PostMapping("/sync")
  @Operation(summary = "Sync the first N catalog entries (1 to 20, default 10)")
  @ApiResponse(responseCode = "200", description = "Per-entry outcome and counts")
  public BulkSyncResponse syncFirst(
      @Parameter(description = "How many catalog entries, 1 to 20", example = "10")
          @RequestParam(defaultValue = "10")
          int limit,
      Authentication caller) {
    return LocalPokemonWebMapper.toResponse(syncCatalogPage.syncFirst(limit, caller.getName()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "One local record")
  @ApiResponse(responseCode = "200", description = "The record, with its version as ETag")
  @ApiResponse(responseCode = "404", description = "Unknown id")
  public ResponseEntity<LocalPokemonResponse> get(@PathVariable String id) {
    LocalPokemon pokemon = getLocalPokemon.get(LocalPokemonId.parse(id));
    return ResponseEntity.ok()
        .eTag(LocalPokemonWebMapper.etag(pokemon))
        .body(LocalPokemonWebMapper.toResponse(pokemon));
  }
}
