package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import com.ballastlane.pokedex.adapter.in.web.ReadOnlyFields;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code PUT /api/v1/local-pokemon/{id}}: every proprietary field; one left out is cleared.
 * Also documents the members a {@code PATCH} may carry. Upstream, identity and audit members are
 * read-only.
 */
@ReadOnlyFields(value = LocalPokemonResponse.class, reason = LocalPokemonResponse.READ_ONLY)
public record ReplaceLocalPokemonRequest(
    @Schema(
            description = "Name per locale, at most 10",
            example = "{\"pt-BR\": \"Pikachu\"}",
            nullable = true)
        Map<String, String> localizedNames,
    @Schema(example = "Kanto", description = "At most 50 characters", nullable = true)
        String region,
    @Schema(description = "At most 500 characters", nullable = true) String habitatNotes,
    @Schema(
            example = "[\"starter-line\"]",
            description = "At most 20 tags of lower-case letters, digits and hyphens",
            nullable = true)
        List<String> internalTags,
    @Schema(description = "At most 2000 characters", nullable = true) String notes) {}
