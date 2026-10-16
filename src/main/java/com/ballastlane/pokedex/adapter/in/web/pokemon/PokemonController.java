package com.ballastlane.pokedex.adapter.in.web.pokemon;

import com.ballastlane.pokedex.adapter.in.web.PageResponse;
import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonNameQuery;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Pokémon catalog (US01) and detail view (US02). Parameter constraints are checked by Spring
 * MVC's built-in method validation (no {@code @Validated}), which reports every invalid parameter
 * in one 400.
 */
@RestController
@RequestMapping(PokemonController.BASE_PATH)
@Tag(name = "Catalog", description = "Browse Pokémon from PokeAPI (public)")
public class PokemonController {

  public static final String BASE_PATH = "/api/v1/pokemon";

  private final ListPokemon listPokemon;
  private final GetPokemonDetail getPokemonDetail;

  public PokemonController(ListPokemon listPokemon, GetPokemonDetail getPokemonDetail) {
    this.listPokemon = listPokemon;
    this.getPokemonDetail = getPokemonDetail;
  }

  @GetMapping
  @Operation(
      summary = "Browse the catalog page by page, or search it by part of a name (US01)",
      description =
          "Without `name`, the whole catalog in Pokédex order. With `name`, only the Pokémon whose"
              + " name contains it (case-insensitive; spaces count as hyphens), in the same order"
              + " and with the same paging.")
  @ApiResponse(responseCode = "200", description = "A page of catalog entries")
  @ApiResponse(responseCode = "400", description = "Invalid page, size or name")
  @ApiResponse(responseCode = "503", description = "PokeAPI is unavailable; retry later")
  public PageResponse<PokemonSummaryResponse> list(
      @Parameter(description = "Zero-based page number", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Entries per page, 1 to 100", example = "20")
          @RequestParam(defaultValue = "" + PageRequest.DEFAULT_SIZE)
          @Min(1)
          @Max(PageRequest.MAX_SIZE)
          int size,
      @Parameter(
              description =
                  "Part of the name to search for: 3 to 50 letters, digits, hyphens or spaces."
                      + " Blank or absent lists the whole catalog.",
              example = "pika")
          @RequestParam(required = false)
          String name) {
    PageRequest request = new PageRequest(page, size);
    return PokemonWebMapper.toResponse(
        PokemonNameQuery.parse(name)
            .map(query -> listPokemon.search(query, request))
            .orElseGet(() -> listPokemon.list(request)));
  }

  @GetMapping("/{idOrName}")
  @Operation(summary = "Detailed view of one Pokémon, by Pokédex number or name (US02)")
  @ApiResponse(responseCode = "200", description = "The Pokémon's detail and evolution tree")
  @ApiResponse(responseCode = "400", description = "Malformed identifier")
  @ApiResponse(responseCode = "404", description = "No Pokémon matches the identifier")
  @ApiResponse(responseCode = "503", description = "PokeAPI is unavailable; retry later")
  public PokemonDetailResponse detail(
      @Parameter(description = "Pokédex number or name (case-insensitive)", example = "eevee")
          @PathVariable
          String idOrName) {
    return PokemonWebMapper.toResponse(getPokemonDetail.get(PokemonIdentifier.parse(idOrName)));
  }
}
