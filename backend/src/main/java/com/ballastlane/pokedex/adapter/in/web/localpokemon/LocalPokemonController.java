package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import com.ballastlane.pokedex.adapter.in.web.PageResponse;
import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemonCommand;
import com.ballastlane.pokedex.application.port.in.localpokemon.DeleteLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.GetLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.ListLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.PatchLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.ReplaceLocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFilter;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * The local replica as a REST resource (US04). Authenticated only; {@code DELETE} needs {@code
 * ADMIN}. Every single-record response carries an {@code ETag} with the record's version, and
 * {@code PUT}/{@code PATCH} require it back in {@code If-Match} (Optimistic Offline Lock): missing
 * is 428, outdated is 409. Importing from PokeAPI lives in {@link LocalPokemonSyncController}.
 */
@RestController
@RequestMapping(LocalPokemonController.BASE_PATH)
@Tag(name = "Local Pokémon", description = "Locally owned copies with proprietary fields")
@SecurityRequirement(name = "bearer-jwt")
public class LocalPokemonController {

  public static final String BASE_PATH = "/api/v1/local-pokemon";
  static final String MERGE_PATCH_JSON = "application/merge-patch+json";
  private static final int MAX_FILTER_LENGTH = 50;

  private final GetLocalPokemon getLocalPokemon;
  private final ListLocalPokemon listLocalPokemon;
  private final CreateLocalPokemon createLocalPokemon;
  private final ReplaceLocalPokemon replaceLocalPokemon;
  private final PatchLocalPokemon patchLocalPokemon;
  private final DeleteLocalPokemon deleteLocalPokemon;

  public LocalPokemonController(
      GetLocalPokemon getLocalPokemon,
      ListLocalPokemon listLocalPokemon,
      CreateLocalPokemon createLocalPokemon,
      ReplaceLocalPokemon replaceLocalPokemon,
      PatchLocalPokemon patchLocalPokemon,
      DeleteLocalPokemon deleteLocalPokemon) {
    this.getLocalPokemon = getLocalPokemon;
    this.listLocalPokemon = listLocalPokemon;
    this.createLocalPokemon = createLocalPokemon;
    this.replaceLocalPokemon = replaceLocalPokemon;
    this.patchLocalPokemon = patchLocalPokemon;
    this.deleteLocalPokemon = deleteLocalPokemon;
  }

  @GetMapping
  @Operation(summary = "Page through local records by PokeAPI number, optionally filtered")
  @ApiResponse(responseCode = "200", description = "A page of local records")
  @ApiResponse(responseCode = "400", description = "Invalid page, size or filter")
  public PageResponse<LocalPokemonResponse> list(
      @Parameter(description = "Zero-based page number", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Records per page, 1 to 100", example = "20")
          @RequestParam(defaultValue = "" + PageRequest.DEFAULT_SIZE)
          @Min(1)
          @Max(PageRequest.MAX_SIZE)
          int size,
      @Parameter(
              description = "Only records with this internal tag (any case)",
              example = "starter")
          @RequestParam(required = false)
          @Size(max = MAX_FILTER_LENGTH)
          String tag,
      @Parameter(description = "Only records whose name contains this (any case)", example = "saur")
          @RequestParam(required = false)
          @Size(max = MAX_FILTER_LENGTH)
          String name) {
    return LocalPokemonWebMapper.toResponse(
        listLocalPokemon.list(new LocalPokemonFilter(tag, name), new PageRequest(page, size)));
  }

  @PostMapping
  @Operation(summary = "Store a Pokémon locally: upstream fields from PokeAPI plus yours")
  @ApiResponse(responseCode = "201", description = "Stored; Location points to the new record")
  @ApiResponse(responseCode = "400", description = "Invalid, unknown or read-only fields")
  @ApiResponse(responseCode = "409", description = "This PokeAPI number is already stored")
  @ApiResponse(responseCode = "503", description = "PokeAPI unavailable; nothing stored")
  public ResponseEntity<LocalPokemonResponse> create(
      @Valid @RequestBody CreateLocalPokemonRequest body, Authentication caller) {
    LocalPokemon created =
        createLocalPokemon.create(
            new CreateLocalPokemonCommand(body.pokeapiId(), LocalPokemonWebMapper.toValues(body)),
            caller.getName());
    return withEtag(
        ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id().value())), created);
  }

  @GetMapping("/{id}")
  @Operation(summary = "One local record")
  @ApiResponse(responseCode = "200", description = "The record, with its version as ETag")
  @ApiResponse(responseCode = "404", description = "Unknown id")
  public ResponseEntity<LocalPokemonResponse> get(@PathVariable String id) {
    return ok(getLocalPokemon.get(LocalPokemonId.parse(id)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Replace every proprietary field (left-out fields are cleared)")
  @ApiResponse(responseCode = "200", description = "Replaced; ETag holds the new version")
  @ApiResponse(responseCode = "400", description = "Invalid or read-only fields")
  @ApiResponse(responseCode = "404", description = "Unknown id")
  @ApiResponse(responseCode = "409", description = "Changed since your ETag: reload and retry")
  @ApiResponse(responseCode = "428", description = "If-Match is missing")
  public ResponseEntity<LocalPokemonResponse> replace(
      @PathVariable String id,
      @Parameter(description = "ETag you last read, e.g. \"3\"", required = true)
          @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
          String ifMatch,
      @Valid @RequestBody ReplaceLocalPokemonRequest body) {
    LocalPokemonId localId = LocalPokemonId.parse(id);
    return ok(
        replaceLocalPokemon.replace(
            localId, IfMatch.required(ifMatch), LocalPokemonWebMapper.toValues(body)));
  }

  @PatchMapping(
      value = "/{id}",
      consumes = {MERGE_PATCH_JSON, MediaType.APPLICATION_JSON_VALUE})
  @Operation(
      summary = "Change some proprietary fields (JSON Merge Patch, RFC 7396)",
      description =
          "Absent members are kept, null clears, a value replaces; localizedNames merges per"
              + " locale and a locale set to null is removed.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = MERGE_PATCH_JSON,
              schema = @Schema(implementation = ReplaceLocalPokemonRequest.class)))
  @ApiResponse(responseCode = "200", description = "Patched; ETag holds the new version")
  @ApiResponse(responseCode = "400", description = "Invalid, unknown or read-only members")
  @ApiResponse(responseCode = "404", description = "Unknown id")
  @ApiResponse(responseCode = "409", description = "Changed since your ETag: reload and retry")
  @ApiResponse(responseCode = "428", description = "If-Match is missing")
  public ResponseEntity<LocalPokemonResponse> patch(
      @PathVariable String id,
      @Parameter(description = "ETag you last read, e.g. \"3\"", required = true)
          @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
          String ifMatch,
      @RequestBody JsonNode body) {
    LocalPokemonId localId = LocalPokemonId.parse(id);
    return ok(
        patchLocalPokemon.patch(localId, IfMatch.required(ifMatch), MergePatchReader.read(body)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a local record (ADMIN)")
  @ApiResponse(responseCode = "204", description = "Deleted")
  @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
  @ApiResponse(responseCode = "404", description = "Unknown id")
  @ApiResponse(responseCode = "409", description = "Changed since the If-Match ETag")
  public ResponseEntity<Void> delete(
      @PathVariable String id,
      @Parameter(description = "Optional: delete only if still at this ETag")
          @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
          String ifMatch) {
    LocalPokemonId localId = LocalPokemonId.parse(id);
    deleteLocalPokemon.delete(localId, IfMatch.optional(ifMatch));
    return ResponseEntity.noContent().build();
  }

  private static ResponseEntity<LocalPokemonResponse> ok(LocalPokemon pokemon) {
    return withEtag(ResponseEntity.ok(), pokemon);
  }

  private static ResponseEntity<LocalPokemonResponse> withEtag(
      ResponseEntity.BodyBuilder response, LocalPokemon pokemon) {
    return response
        .eTag(LocalPokemonWebMapper.etag(pokemon))
        .body(LocalPokemonWebMapper.toResponse(pokemon));
  }
}
