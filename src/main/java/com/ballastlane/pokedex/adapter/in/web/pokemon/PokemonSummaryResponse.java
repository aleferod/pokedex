package com.ballastlane.pokedex.adapter.in.web.pokemon;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** One catalog entry (US01). */
public record PokemonSummaryResponse(
    @Schema(example = "1", description = "Pokédex number") int id,
    @Schema(example = "bulbasaur") String name,
    @Schema(
            example =
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
            nullable = true)
        String spriteUrl,
    @Schema(example = "Seed Pokémon", description = "English genus", nullable = true)
        String category,
    @Schema(example = "6.9", description = "Weight in kilograms") BigDecimal weight,
    @Schema(example = "[\"overgrow\", \"chlorophyll\"]", description = "Abilities in slot order")
        List<String> abilities) {}
