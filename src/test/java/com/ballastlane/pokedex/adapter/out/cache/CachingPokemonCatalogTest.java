package com.ballastlane.pokedex.adapter.out.cache;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.localpokemon.UpstreamData;
import com.ballastlane.pokedex.domain.pokemon.CatalogName;
import com.ballastlane.pokedex.domain.pokemon.EvolutionStage;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;

/** The decorator over real Caffeine-backed Spring caches whose clock the test controls. */
class CachingPokemonCatalogTest {

  private static final Duration TTL = Duration.ofHours(1);
  private static final PageRequest FIRST_THREE = new PageRequest(0, 3);
  private static final PokemonIdentifier PIKACHU = PokemonIdentifier.parse("pikachu");
  private static final PokemonIdentifier NUMBER_25 = PokemonIdentifier.parse("25");

  private final AtomicLong nanos = new AtomicLong();
  private final PokemonCatalogPort source = mock(PokemonCatalogPort.class);
  private CachingPokemonCatalog catalog;

  @BeforeEach
  void setUp() {
    catalog = new CachingPokemonCatalog(source, cache("pages"), cache("details"), cache("names"));
  }

  @Test
  void should_askTheSourceOncePerPage_when_pagesAreRequestedAgain() {
    // Given
    Page<PokemonSummary> page = Page.of(List.of(), FIRST_THREE, 1350);
    given(source.findPage(FIRST_THREE)).willReturn(page);
    given(source.findPage(new PageRequest(0, 5)))
        .willReturn(Page.of(List.of(), new PageRequest(0, 5), 1350));

    // When
    Page<PokemonSummary> first = catalog.findPage(FIRST_THREE);
    Page<PokemonSummary> second = catalog.findPage(FIRST_THREE);
    catalog.findPage(new PageRequest(0, 5));

    // Then
    assertThat(second).isEqualTo(first).isEqualTo(page);
    then(source).should(times(1)).findPage(FIRST_THREE);
    then(source).should(times(1)).findPage(new PageRequest(0, 5));
  }

  @Test
  void should_askTheSourceOnceForTheNameIndex_when_searchedAgain() {
    // Given
    List<CatalogName> names =
        List.of(
            new CatalogName(new PokemonId(25), "pikachu"),
            new CatalogName(new PokemonId(26), "raichu"));
    given(source.findAllNames()).willReturn(names);

    // When
    List<CatalogName> first = catalog.findAllNames();
    List<CatalogName> second = catalog.findAllNames();

    // Then
    assertThat(second).isEqualTo(first).isEqualTo(names);
    then(source).should(times(1)).findAllNames();
  }

  @Test
  void should_askTheSourceOncePerSetOfNumbers_when_summariesAreRequestedAgain() {
    // Given
    List<PokemonId> pikachuAndRaichu = List.of(new PokemonId(25), new PokemonId(26));
    List<PokemonId> raichuOnly = List.of(new PokemonId(26));
    given(source.findSummaries(pikachuAndRaichu)).willReturn(List.of());
    given(source.findSummaries(raichuOnly)).willReturn(List.of());

    // When
    catalog.findSummaries(pikachuAndRaichu);
    catalog.findSummaries(List.of(new PokemonId(25), new PokemonId(26)));
    catalog.findSummaries(raichuOnly);

    // Then
    then(source).should(times(1)).findSummaries(pikachuAndRaichu);
    then(source).should(times(1)).findSummaries(raichuOnly);
  }

  @Test
  void should_askTheSourceAgain_when_theEntryHasExpired() {
    given(source.findPage(FIRST_THREE)).willReturn(Page.of(List.of(), FIRST_THREE, 1350));
    catalog.findPage(FIRST_THREE);

    nanos.addAndGet(TTL.toNanos() - 1);
    catalog.findPage(FIRST_THREE);
    then(source).should(times(1)).findPage(FIRST_THREE);

    nanos.addAndGet(2);
    catalog.findPage(FIRST_THREE);
    then(source).should(times(2)).findPage(FIRST_THREE);
  }

  @Test
  void should_rememberThatAPokemonIsUnknown_when_askedTwice() {
    PokemonIdentifier missingno = PokemonIdentifier.parse("missingno");
    given(source.findDetail(missingno)).willReturn(Optional.empty());

    assertThat(catalog.findDetail(missingno)).isEmpty();
    assertThat(catalog.findDetail(missingno)).isEmpty();
    then(source).should(times(1)).findDetail(missingno);
  }

  @Test
  void should_rethrowTheFailureAndCacheNothing_when_theSourceFails() {
    // Given a source that fails once, then recovers
    CatalogUnavailableException outage = new CatalogUnavailableException("PokeAPI down", null);
    Page<PokemonSummary> page = Page.of(List.of(), FIRST_THREE, 1350);
    given(source.findPage(FIRST_THREE)).willThrow(outage).willReturn(page);

    // When / Then: the very same exception, not a cache wrapper
    assertThatThrownBy(() -> catalog.findPage(FIRST_THREE)).isSameAs(outage);
    assertThat(catalog.findPage(FIRST_THREE)).isEqualTo(page);
    then(source).should(times(2)).findPage(FIRST_THREE);
  }

  @Test
  void should_serveTheNumberToo_when_loadedByName() {
    given(source.findDetail(PIKACHU)).willReturn(Optional.of(detail(60)));

    assertThat(catalog.findDetail(PIKACHU)).contains(detail(60));
    assertThat(catalog.findDetail(NUMBER_25)).contains(detail(60));
    then(source).should(times(1)).findDetail(any());
  }

  @Test
  void should_serveTheNameToo_when_loadedByNumber() {
    given(source.findDetail(NUMBER_25)).willReturn(Optional.of(detail(60)));

    catalog.findDetail(NUMBER_25);

    assertThat(catalog.findDetail(PIKACHU)).contains(detail(60));
    then(source).should(times(1)).findDetail(any());
  }

  @Test
  void should_bypassTheCacheAndKeepWhatItRead_when_aFreshDetailIsRequested() {
    // Given Pikachu cached at 6.0 kg
    given(source.findDetail(NUMBER_25)).willReturn(Optional.of(detail(60)));
    catalog.findDetail(NUMBER_25);
    given(source.findFreshDetail(NUMBER_25)).willReturn(Optional.of(detail(61)));

    // When
    Optional<PokemonDetail> fresh = catalog.findFreshDetail(NUMBER_25);

    // Then the source was read, and both keys now serve the new data
    assertThat(fresh).contains(detail(61));
    assertThat(catalog.findDetail(NUMBER_25)).contains(detail(61));
    assertThat(catalog.findDetail(PIKACHU)).contains(detail(61));
    then(source).should(times(1)).findDetail(any());
  }

  @Test
  void should_keepTheCachedValue_when_aFreshReadFails() {
    given(source.findDetail(NUMBER_25)).willReturn(Optional.of(detail(60)));
    catalog.findDetail(NUMBER_25);
    given(source.findFreshDetail(NUMBER_25))
        .willThrow(new CatalogUnavailableException("PokeAPI down", null));

    assertThatThrownBy(() -> catalog.findFreshDetail(NUMBER_25))
        .isInstanceOf(CatalogUnavailableException.class);
    assertThat(catalog.findDetail(NUMBER_25)).contains(detail(60));
    then(source).should(times(1)).findDetail(any());
  }

  @Test
  void should_notCacheAFreshMiss_when_theSourceDoesNotKnowThePokemon() {
    PokemonIdentifier missingno = PokemonIdentifier.parse("missingno");
    given(source.findFreshDetail(missingno)).willReturn(Optional.empty());
    given(source.findDetail(missingno)).willReturn(Optional.empty());

    assertThat(catalog.findFreshDetail(missingno)).isEmpty();
    assertThat(catalog.findDetail(missingno)).isEmpty();
    then(source).should(times(1)).findDetail(missingno);
    then(source).should(never()).findPage(any());
  }

  private CaffeineCache cache(String name) {
    return new CaffeineCache(
        name,
        Caffeine.newBuilder()
            .ticker(nanos::get)
            .executor(Runnable::run)
            .expireAfterWrite(TTL)
            .maximumSize(100)
            .build(),
        false);
  }

  private static PokemonDetail detail(int weightHectograms) {
    UpstreamData upstream = pikachu(weightHectograms);
    return new PokemonDetail(
        new PokemonId(25),
        upstream.name(),
        upstream.spriteUrl(),
        upstream.imageUrl(),
        upstream.category(),
        "It stores electricity in its cheeks.",
        upstream.types(),
        upstream.height(),
        upstream.weight(),
        upstream.abilities(),
        upstream.stats(),
        new EvolutionStage(new PokemonId(172), "pichu", null, List.of()));
  }
}
