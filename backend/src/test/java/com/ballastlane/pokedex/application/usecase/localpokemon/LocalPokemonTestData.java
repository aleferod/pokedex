package com.ballastlane.pokedex.application.usecase.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;

import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.InternalTags;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNames;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Records shared by the edit use-case tests. */
final class LocalPokemonTestData {

  static final Instant IMPORTED = Instant.parse("2026-10-14T12:00:00Z");
  static final Instant NOW = Instant.parse("2026-10-15T09:00:00Z");

  private LocalPokemonTestData() {}

  /** Pikachu at {@code version}: region Kanto, notes "old", two names and one tag. */
  static LocalPokemon storedPikachu(long version) {
    return new LocalPokemon(
        LocalPokemonId.random(),
        new PokemonId(25),
        pikachu(60),
        new ProprietaryData(
            LocalizedNames.of(Map.of("pt-BR", "Pikachu", "ja", "ピカチュウ")),
            "Kanto",
            null,
            InternalTags.of(List.of("starter-line")),
            "old"),
        new AuditInfo(IMPORTED, IMPORTED, IMPORTED, "ash"),
        version);
  }

  static LocalPokemon withVersion(LocalPokemon pokemon, long version) {
    return new LocalPokemon(
        pokemon.id(),
        pokemon.pokeapiId(),
        pokemon.upstream(),
        pokemon.proprietary(),
        pokemon.audit(),
        version);
  }
}
