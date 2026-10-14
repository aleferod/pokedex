package com.ballastlane.pokedex.domain.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LocalPokemonTest {

  private static final Instant IMPORTED = Instant.parse("2026-10-14T12:00:00Z");
  private static final Instant RESYNCED = Instant.parse("2026-10-15T08:30:00Z");

  @Test
  void should_startWithEmptyProprietaryDataAndEqualTimestamps_when_imported() {
    // When
    LocalPokemon imported =
        LocalPokemon.importFrom(
            LocalPokemonId.random(), new PokemonId(25), pikachu(60), "ash", IMPORTED);

    // Then
    assertThat(imported.pokeapiId()).isEqualTo(new PokemonId(25));
    assertThat(imported.upstream()).isEqualTo(pikachu(60));
    assertThat(imported.proprietary()).isEqualTo(ProprietaryData.empty());
    assertThat(imported.audit()).isEqualTo(new AuditInfo(IMPORTED, IMPORTED, IMPORTED, "ash"));
    assertThat(imported.version()).isNull();
  }

  @Test
  void should_replaceOnlyUpstreamData_when_resynced() {
    // Given a stored record with proprietary data
    ProprietaryData owned =
        new ProprietaryData(
            LocalizedNames.of(Map.of("pt-BR", "Pikachu")),
            "Kanto",
            "Viridian Forest",
            InternalTags.of(List.of("starter-line")),
            "Ash's partner");
    LocalPokemon stored =
        new LocalPokemon(
            LocalPokemonId.random(),
            new PokemonId(25),
            pikachu(60),
            owned,
            new AuditInfo(IMPORTED, IMPORTED, IMPORTED, "ash"),
            3L);

    // When PokeAPI reports a new weight
    LocalPokemon resynced = stored.resyncWith(pikachu(61), RESYNCED);

    // Then
    assertThat(resynced.upstream().weight().hectograms()).isEqualTo(61);
    assertThat(resynced.proprietary()).isEqualTo(owned);
    assertThat(resynced.audit()).isEqualTo(new AuditInfo(IMPORTED, RESYNCED, RESYNCED, "ash"));
    assertThat(resynced.id()).isEqualTo(stored.id());
    assertThat(resynced.pokeapiId()).isEqualTo(stored.pokeapiId());
    assertThat(resynced.version()).isEqualTo(3L);
  }
}
