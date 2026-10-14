package com.ballastlane.pokedex.adapter.out.persistence.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.InternalTags;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNames;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.support.PostgresTestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PostgresTestcontainersConfiguration.class, LocalPokemonPersistenceAdapter.class})
class LocalPokemonPersistenceAdapterIT {

  private static final Instant IMPORTED = Instant.parse("2026-10-14T12:00:00.123456Z");
  private static final Instant RESYNCED = Instant.parse("2026-10-15T08:30:00.654321Z");

  @Autowired private LocalPokemonPersistenceAdapter adapter;
  @Autowired private EntityManager entityManager;

  @Test
  void should_storeAndReadBackEveryField_when_newRecordIsSaved() {
    // Given a record with upstream arrays and proprietary data
    LocalPokemon pikachu = pikachuWithProprietaryData();

    // When
    LocalPokemon saved = adapter.save(pikachu);
    entityManager.clear();

    // Then
    assertThat(saved.version()).isZero();
    LocalPokemon reloaded = adapter.findById(pikachu.id()).orElseThrow();
    assertThat(reloaded.upstream()).isEqualTo(pikachu.upstream());
    assertThat(reloaded.proprietary()).isEqualTo(pikachu.proprietary());
    assertThat(reloaded.audit()).isEqualTo(pikachu.audit());
    assertThat(reloaded.version()).isZero();
    assertThat(adapter.findByPokeapiId(new PokemonId(25))).contains(reloaded);
    assertThat(adapter.findByPokeapiId(new PokemonId(26))).isEmpty();
    assertThat(adapter.findById(LocalPokemonId.random())).isEmpty();
  }

  @Test
  void should_incrementVersionAndKeepProprietaryData_when_resynced() {
    // Given
    LocalPokemon stored = adapter.save(pikachuWithProprietaryData());
    entityManager.clear();

    // When
    LocalPokemon resynced = adapter.save(stored.resyncWith(pikachu(61), RESYNCED));
    entityManager.clear();

    // Then
    assertThat(resynced.version()).isEqualTo(1L);
    LocalPokemon reloaded = adapter.findById(stored.id()).orElseThrow();
    assertThat(reloaded.upstream().weight().hectograms()).isEqualTo(61);
    assertThat(reloaded.proprietary()).isEqualTo(stored.proprietary());
    assertThat(reloaded.audit().syncedAt()).isEqualTo(RESYNCED);
    assertThat(reloaded.audit().createdAt()).isEqualTo(IMPORTED);
  }

  @Test
  void should_throwStale_when_savingAnOutdatedVersion() {
    // Given a record updated once since this copy was read
    LocalPokemon readEarlier = adapter.save(pikachuWithProprietaryData());
    entityManager.clear();
    adapter.save(readEarlier.resyncWith(pikachu(61), RESYNCED));
    entityManager.clear();

    // When / Then
    assertThatThrownBy(() -> adapter.save(readEarlier.resyncWith(pikachu(62), RESYNCED)))
        .isInstanceOf(StaleLocalPokemonException.class);
  }

  @Test
  void should_throwDuplicate_when_anotherRecordHasThePokeApiNumber() {
    // Given
    adapter.save(pikachuWithProprietaryData());
    entityManager.clear();

    // When / Then
    LocalPokemon second =
        LocalPokemon.importFrom(
            LocalPokemonId.random(), new PokemonId(25), pikachu(60), "misty", IMPORTED);
    assertThatThrownBy(() -> adapter.save(second))
        .isInstanceOf(DuplicateLocalPokemonException.class);
  }

  private static LocalPokemon pikachuWithProprietaryData() {
    return new LocalPokemon(
        LocalPokemonId.random(),
        new PokemonId(25),
        pikachu(60),
        new ProprietaryData(
            LocalizedNames.of(Map.of("pt-BR", "Pikachu", "ja", "ピカチュウ")),
            "Kanto",
            "Viridian Forest",
            InternalTags.of(List.of("starter-line", "electric")),
            "Ash's partner"),
        new AuditInfo(IMPORTED, IMPORTED, IMPORTED, "ash"),
        null);
  }
}
