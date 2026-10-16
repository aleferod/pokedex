package com.ballastlane.pokedex.adapter.out.persistence.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.InternalTags;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFilter;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNames;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.UpstreamData;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import com.ballastlane.pokedex.support.PostgresTestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PostgresTestcontainersConfiguration.class, LocalPokemonPersistenceAdapter.class})
class LocalPokemonPersistenceAdapterIT {

  private static final Instant IMPORTED = Instant.parse("2026-10-14T12:00:00.123456Z");
  private static final Instant RESYNCED = Instant.parse("2026-10-15T08:30:00.654321Z");
  private static final PageRequest FIRST_20 = new PageRequest(0, 20);

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

  @Test
  void should_matchTagsExactlyIgnoringCase_when_filteringByTag() {
    // Given
    store(1, "bulbasaur", "starter");
    store(4, "charmander", "starter", "kanto");
    store(25, "pikachu");

    // When
    Page<LocalPokemon> starters =
        adapter.findAll(new LocalPokemonFilter("STARTER", null), FIRST_20);

    // Then
    assertThat(numbers(starters)).containsExactly(1, 4);
    assertThat(starters.totalElements()).isEqualTo(2);
    assertThat(adapter.findAll(new LocalPokemonFilter("start", null), FIRST_20).content())
        .isEmpty();
  }

  @Test
  void should_matchNameSubstringsIgnoringCase_when_filteringByName() {
    store(3, "venusaur");
    store(1, "bulbasaur");
    store(25, "pikachu");
    store(2, "ivysaur");

    assertThat(numbers(adapter.findAll(new LocalPokemonFilter(null, "SAUR"), FIRST_20)))
        .containsExactly(1, 2, 3);
  }

  @Test
  void should_treatWildcardsLiterally_when_filteringByName() {
    store(1, "bulbasaur");
    store(122, "mr-mime");

    assertThat(adapter.findAll(new LocalPokemonFilter(null, "%"), FIRST_20).content()).isEmpty();
    assertThat(adapter.findAll(new LocalPokemonFilter(null, "_"), FIRST_20).content()).isEmpty();
    assertThat(adapter.findAll(new LocalPokemonFilter(null, "\\"), FIRST_20).content()).isEmpty();
    assertThat(numbers(adapter.findAll(new LocalPokemonFilter(null, "r-m"), FIRST_20)))
        .containsExactly(122);
  }

  @Test
  void should_requireEveryCriterion_when_filtersAreCombined() {
    store(1, "bulbasaur", "starter");
    store(4, "charmander", "starter");
    store(5, "charmeleon");

    assertThat(numbers(adapter.findAll(new LocalPokemonFilter("starter", "char"), FIRST_20)))
        .containsExactly(4);
  }

  @Test
  void should_pageInPokeApiOrder_when_listingEverything() {
    // Given records stored out of order
    for (int number : new int[] {25, 1, 4, 7, 2}) {
      store(number, "pokemon-" + number);
    }

    // When
    Page<LocalPokemon> first = adapter.findAll(LocalPokemonFilter.none(), new PageRequest(0, 2));
    Page<LocalPokemon> middle = adapter.findAll(LocalPokemonFilter.none(), new PageRequest(1, 2));
    Page<LocalPokemon> last = adapter.findAll(LocalPokemonFilter.none(), new PageRequest(2, 2));

    // Then
    assertThat(numbers(first)).containsExactly(1, 2);
    assertThat(numbers(middle)).containsExactly(4, 7);
    assertThat(numbers(last)).containsExactly(25);
    assertThat(List.of(first, middle, last))
        .allSatisfy(
            page -> {
              assertThat(page.totalElements()).isEqualTo(5);
              assertThat(page.totalPages()).isEqualTo(3);
            });
  }

  @Test
  void should_loadLocalizedNamesWithoutOneQueryPerRecord_when_listing() {
    // Given five records, each with localized names
    for (int number = 1; number <= 5; number++) {
      store(number, "pokemon-" + number);
    }
    Statistics statistics =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    // When
    Page<LocalPokemon> page = adapter.findAll(LocalPokemonFilter.none(), FIRST_20);

    // Then the names are there, from one page query plus one batched names query (no N+1)
    assertThat(page.content())
        .hasSize(5)
        .allSatisfy(p -> assertThat(p.proprietary().localizedNames().names()).containsKey("pt-BR"));
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
  }

  @Test
  void should_tellWhetherAPokeApiNumberIsStored() {
    store(25, "pikachu");

    assertThat(adapter.existsByPokeapiId(new PokemonId(25))).isTrue();
    assertThat(adapter.existsByPokeapiId(new PokemonId(26))).isFalse();
  }

  @Test
  void should_removeTheRecordAndItsNames_when_deleted() {
    // Given
    LocalPokemon stored = store(25, "pikachu");

    // When
    adapter.delete(stored);
    entityManager.clear();

    // Then
    assertThat(adapter.findById(stored.id())).isEmpty();
    assertThat(
            entityManager
                .createNativeQuery(
                    "SELECT count(*) FROM local_pokemon_localized_name WHERE local_pokemon_id = ?1")
                .setParameter(1, stored.id().value())
                .getSingleResult())
        .isEqualTo(0L);
  }

  @Test
  void should_throwStaleAndKeepTheRecord_when_deletingAnOutdatedVersion() {
    // Given a record updated once since this copy was read
    LocalPokemon readEarlier = store(25, "pikachu");
    adapter.save(readEarlier.resyncWith(pikachu(61), RESYNCED));
    entityManager.clear();

    // When / Then
    assertThatThrownBy(() -> adapter.delete(readEarlier))
        .isInstanceOf(StaleLocalPokemonException.class);
    entityManager.clear();
    assertThat(adapter.findById(readEarlier.id())).isPresent();
  }

  private LocalPokemon store(int number, String name, String... tags) {
    UpstreamData pikachu = pikachu(60);
    UpstreamData upstream =
        new UpstreamData(
            name,
            pikachu.spriteUrl(),
            pikachu.imageUrl(),
            pikachu.category(),
            pikachu.types(),
            pikachu.height(),
            pikachu.weight(),
            pikachu.abilities(),
            pikachu.stats());
    LocalPokemon stored =
        adapter.save(
            new LocalPokemon(
                LocalPokemonId.random(),
                new PokemonId(number),
                upstream,
                new ProprietaryData(
                    LocalizedNames.of(Map.of("pt-BR", name)),
                    null,
                    null,
                    InternalTags.of(List.of(tags)),
                    null),
                new AuditInfo(IMPORTED, IMPORTED, IMPORTED, "ash"),
                null));
    entityManager.clear();
    return stored;
  }

  private static List<Integer> numbers(Page<LocalPokemon> page) {
    return page.content().stream().map(p -> p.pokeapiId().value()).toList();
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
