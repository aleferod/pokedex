package com.ballastlane.pokedex.adapter.out.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** The PokeAPI adapter against a WireMock PokeAPI serving real (trimmed) captures. */
class PokeApiCatalogAdapterTest {

  private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(500);
  // Spring applies the read timeout to reading the whole body, so leave room for a cold JVM.
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

  @RegisterExtension
  static final WireMockExtension POKEAPI =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Test
  void should_returnMappedEntriesInOrderWithTotals_when_firstPageIsRequested() {
    // Given
    stubFirstThree();

    // When
    Page<PokemonSummary> page = adapter(8).findPage(new PageRequest(0, 3));

    // Then
    assertThat(page.content())
        .extracting(PokemonSummary::name)
        .containsExactly("bulbasaur", "ivysaur", "venusaur");
    assertThat(page.content()).extracting(PokemonSummary::category).containsOnly("Seed Pokémon");
    assertThat(page.totalElements()).isEqualTo(1351);
    assertThat(page.totalPages()).isEqualTo(451);
  }

  @Test
  void should_returnEmptyContentWithTotals_when_pageIsPastTheEnd() {
    // Given
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon"))
            .willReturn(okJson("{\"count\":1351,\"next\":null,\"previous\":null,\"results\":[]}")));

    // When
    Page<PokemonSummary> page = adapter(8).findPage(new PageRequest(500, 20));

    // Then
    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isEqualTo(1351);
    assertThat(page.totalPages()).isEqualTo(68);
    POKEAPI.verify(
        getRequestedFor(urlPathEqualTo("/api/v2/pokemon"))
            .withQueryParam("offset", matching("10000"))
            .withQueryParam("limit", matching("20")));
  }

  @Test
  void should_identifyItself_when_callingPokeApi() {
    stubFirstThree();

    adapter(8).findPage(new PageRequest(0, 3));

    POKEAPI.verify(
        getRequestedFor(urlPathEqualTo("/api/v2/pokemon"))
            .withHeader("User-Agent", matching("pokedex-api.*")));
  }

  @Test
  void should_throwUnavailable_when_pokeApiAnswersWithServerError() {
    POKEAPI.stubFor(get(urlPathEqualTo("/api/v2/pokemon")).willReturn(aResponse().withStatus(500)));

    assertThatThrownBy(() -> adapter(8).findPage(new PageRequest(0, 3)))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  @Test
  void should_throwUnavailable_when_aListedEntryCannotBeFetched() {
    stubFirstThree();
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/2")).willReturn(aResponse().withStatus(404)));

    assertThatThrownBy(() -> adapter(8).findPage(new PageRequest(0, 3)))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  @Test
  void should_throwUnavailable_when_pokeApiRefusesConnections() throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    PokeApiCatalogAdapter unreachable =
        new PokeApiCatalogAdapter(
            PokeApiClient.create(
                "http://localhost:" + closedPort + "/api/v2",
                CONNECT_TIMEOUT,
                READ_TIMEOUT,
                "pokedex-api-test"),
            new BoundedFanOut(8));

    assertThatThrownBy(() -> unreachable.findPage(new PageRequest(0, 3)))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  @Test
  void should_throwUnavailableWithinTheReadTimeout_when_pokeApiIsTooSlow() {
    // Given
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon"))
            .willReturn(
                okJson(PokeApiFixtures.json("list-offset-0-limit-3.json")).withFixedDelay(5_000)));
    Instant start = Instant.now();

    // When / Then
    assertThatThrownBy(() -> adapter(8).findPage(new PageRequest(0, 3)))
        .isInstanceOf(CatalogUnavailableException.class);
    assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofMillis(4_500));
  }

  @Test
  void should_fetchDetailsInParallel_when_detailCallsAreSlow() {
    // Given 10 entries whose 20 detail calls take 300 ms each (6 s if fetched one by one)
    String results =
        IntStream.rangeClosed(1, 10)
            .mapToObj(
                i ->
                    "{\"name\":\"p%d\",\"url\":\"https://pokeapi.co/api/v2/pokemon/%d/\"}"
                        .formatted(i, i))
            .collect(Collectors.joining(","));
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon"))
            .willReturn(okJson("{\"count\":1351,\"results\":[" + results + "]}")));
    POKEAPI.stubFor(
        get(urlPathMatching("/api/v2/pokemon/\\d+"))
            .willReturn(okJson(PokeApiFixtures.json("pokemon-1.json")).withFixedDelay(300)));
    POKEAPI.stubFor(
        get(urlPathMatching("/api/v2/pokemon-species/\\d+"))
            .willReturn(okJson(PokeApiFixtures.json("species-1.json")).withFixedDelay(300)));
    Instant start = Instant.now();

    // When
    Page<PokemonSummary> page = adapter(10).findPage(new PageRequest(0, 10));

    // Then
    assertThat(page.content()).hasSize(10);
    assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(3));
  }

  // ---- detail (US02) -------------------------------------------------------------------------

  @Test
  void should_returnTheDetail_when_lookedUpByNumber() {
    stubDetail("1", 1, 1, 1);

    Optional<PokemonDetail> detail = adapter(8).findDetail(PokemonIdentifier.parse("1"));

    assertThat(detail)
        .hasValueSatisfying(
            bulbasaur -> {
              assertThat(bulbasaur.name()).isEqualTo("bulbasaur");
              assertThat(bulbasaur.evolutionChain().evolvesTo()).hasSize(1);
            });
  }

  @Test
  void should_askPokeApiInLowerCase_when_lookedUpByNameInAnyCase() {
    stubDetail("pikachu", 25, 25, 10);

    Optional<PokemonDetail> detail = adapter(8).findDetail(PokemonIdentifier.parse("PIKACHU"));

    assertThat(detail).map(PokemonDetail::id).contains(new PokemonId(25));
    POKEAPI.verify(getRequestedFor(urlPathEqualTo("/api/v2/pokemon/pikachu")));
  }

  @Test
  void should_returnEmpty_when_pokeApiDoesNotKnowThePokemon() {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/missingno")).willReturn(aResponse().withStatus(404)));

    assertThat(adapter(8).findDetail(PokemonIdentifier.parse("missingno"))).isEmpty();
  }

  @Test
  void should_throwUnavailable_when_anExistingPokemonHasNoSpecies() {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/1"))
            .willReturn(okJson(PokeApiFixtures.json("pokemon-1.json"))));
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon-species/1")).willReturn(aResponse().withStatus(404)));

    assertThatThrownBy(() -> adapter(8).findDetail(PokemonIdentifier.parse("1")))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  @Test
  void should_throwUnavailable_when_pokeApiFailsOnTheDetail() {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/1")).willReturn(aResponse().withStatus(500)));

    assertThatThrownBy(() -> adapter(8).findDetail(PokemonIdentifier.parse("1")))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  private static void stubDetail(String pathSegment, int pokemon, int species, int chain) {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/" + pathSegment))
            .willReturn(okJson(PokeApiFixtures.json("pokemon-" + pokemon + ".json"))));
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon-species/" + species))
            .willReturn(okJson(PokeApiFixtures.json("species-" + species + ".json"))));
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/evolution-chain/" + chain))
            .willReturn(okJson(PokeApiFixtures.json("evolution-chain-" + chain + ".json"))));
  }

  private static PokeApiCatalogAdapter adapter(int maxConcurrency) {
    return new PokeApiCatalogAdapter(
        PokeApiClient.create(
            POKEAPI.baseUrl() + "/api/v2", CONNECT_TIMEOUT, READ_TIMEOUT, "pokedex-api-test/1.0"),
        new BoundedFanOut(maxConcurrency));
  }

  private static void stubFirstThree() {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon"))
            .withQueryParam("offset", matching("0"))
            .withQueryParam("limit", matching("3"))
            .willReturn(okJson(PokeApiFixtures.json("list-offset-0-limit-3.json"))));
    for (int id : new int[] {1, 2, 3}) {
      POKEAPI.stubFor(
          get(urlPathEqualTo("/api/v2/pokemon/" + id))
              .willReturn(okJson(PokeApiFixtures.json("pokemon-" + id + ".json"))));
      POKEAPI.stubFor(
          get(urlPathEqualTo("/api/v2/pokemon-species/" + id))
              .willReturn(okJson(PokeApiFixtures.json("species-" + id + ".json"))));
    }
  }
}
