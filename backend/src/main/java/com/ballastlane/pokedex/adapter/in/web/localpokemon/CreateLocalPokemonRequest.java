package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import com.ballastlane.pokedex.adapter.in.web.ReadOnlyFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /api/v1/local-pokemon}: which Pokémon to store plus optional proprietary
 * fields. Upstream fields come from PokeAPI, so sending one is a 400 naming it as read-only. The
 * domain value objects own the proprietary rules; nothing here duplicates them.
 */
@ReadOnlyFields(value = LocalPokemonResponse.class, reason = LocalPokemonResponse.READ_ONLY)
public record CreateLocalPokemonRequest(
    @Schema(example = "1", description = "PokeAPI number of the Pokémon to store")
        @NotNull
        @Positive
        Integer pokeapiId,
    @Schema(
            description = "Name per locale, at most 10",
            example = "{\"pt-BR\": \"Bulbassauro\"}",
            nullable = true)
        Map<String, String> localizedNames,
    @Schema(example = "Kanto", description = "At most 50 characters", nullable = true)
        String region,
    @Schema(description = "At most 500 characters", nullable = true) String habitatNotes,
    @Schema(
            example = "[\"starter\"]",
            description = "At most 20 tags of lower-case letters, digits and hyphens",
            nullable = true)
        List<String> internalTags,
    @Schema(description = "At most 2000 characters", nullable = true) String notes) {}
