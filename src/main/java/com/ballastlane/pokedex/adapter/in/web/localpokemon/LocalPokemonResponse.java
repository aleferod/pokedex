package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import com.ballastlane.pokedex.adapter.in.web.pokemon.PokemonDetailResponse.BaseStatsResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A locally stored Pokémon: PokeAPI snapshot, proprietary fields, audit and version. */
public record LocalPokemonResponse(
    UUID id,
    @Schema(example = "25") int pokeapiId,
    @Schema(description = "Upstream (PokeAPI)", example = "pikachu") String name,
    @Schema(description = "Upstream (PokeAPI)", nullable = true) String spriteUrl,
    @Schema(description = "Upstream (PokeAPI)", nullable = true) String imageUrl,
    @Schema(description = "Upstream (PokeAPI)", nullable = true) String category,
    @Schema(description = "Upstream (PokeAPI)") List<String> types,
    @Schema(description = "Upstream (PokeAPI), metres", example = "0.4") BigDecimal height,
    @Schema(description = "Upstream (PokeAPI), kilograms", example = "6.0") BigDecimal weight,
    @Schema(description = "Upstream (PokeAPI)") List<String> abilities,
    @Schema(description = "Upstream (PokeAPI)") BaseStatsResponse stats,
    @Schema(description = "Proprietary: name per locale", example = "{\"pt-BR\": \"Pikachu\"}")
        Map<String, String> localizedNames,
    @Schema(description = "Proprietary", nullable = true) String region,
    @Schema(description = "Proprietary", nullable = true) String habitatNotes,
    @Schema(description = "Proprietary") List<String> internalTags,
    @Schema(description = "Proprietary", nullable = true) String notes,
    Instant createdAt,
    Instant updatedAt,
    Instant syncedAt,
    String createdBy,
    @Schema(description = "Optimistic-locking version, also sent as the ETag") long version) {}
