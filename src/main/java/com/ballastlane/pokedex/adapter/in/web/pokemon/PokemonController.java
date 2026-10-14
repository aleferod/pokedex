package com.ballastlane.pokedex.adapter.in.web.pokemon;

import com.ballastlane.pokedex.adapter.in.web.PageResponse;
import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Pokémon catalog (US01). Parameter constraints are checked by Spring MVC's built-in method
 * validation (no {@code @Validated}), which reports every invalid parameter in one 400.
 */
@RestController
@RequestMapping(PokemonController.BASE_PATH)
@Tag(name = "Catalog", description = "Browse Pokémon from PokeAPI (public)")
public class PokemonController {

  public static final String BASE_PATH = "/api/v1/pokemon";

  private final ListPokemon listPokemon;

  public PokemonController(ListPokemon listPokemon) {
    this.listPokemon = listPokemon;
  }

  @GetMapping
  @Operation(summary = "Browse the catalog page by page (US01)")
  @ApiResponse(responseCode = "200", description = "A page of catalog entries")
  @ApiResponse(responseCode = "400", description = "Invalid page or size")
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
          int size) {
    return PokemonWebMapper.toResponse(listPokemon.list(new PageRequest(page, size)));
  }
}
