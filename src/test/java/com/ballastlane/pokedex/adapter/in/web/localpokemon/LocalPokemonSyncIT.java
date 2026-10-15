package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiFixtures;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.domain.localpokemon.InternalTags;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNames;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.support.IntegrationTest;
import com.ballastlane.pokedex.support.TestTokens;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/** US03 end to end: sync through HTTP into PostgreSQL, with WireMock standing in for PokeAPI. */
@IntegrationTest
class LocalPokemonSyncIT {

  private static final ParameterizedTypeReference<Map<String, Object>> JSON =
      new ParameterizedTypeReference<>() {};

  @RegisterExtension
  static final WireMockExtension POKEAPI =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private LocalPokemonRepositoryPort repository;
  @Autowired private TokenPort tokenPort;
  @Autowired private JwtEncoder jwtEncoder;

  private String ash;
  private String misty;

  @DynamicPropertySource
  static void pokeApiProperties(DynamicPropertyRegistry registry) {
    registry.add("app.pokeapi.base-url", () -> POKEAPI.baseUrl() + "/api/v2");
  }

  @BeforeEach
  void setUp() {
    jdbc.update("delete from local_pokemon");
    TestTokens tokens = new TestTokens(tokenPort, jwtEncoder);
    ash = tokens.bearer(TestTokens.user("ash", Role.USER));
    misty = tokens.bearer(TestTokens.user("misty", Role.USER));
  }

  @Test
  void should_importAndServeTheRecordAtItsLocation_when_firstSynced() {
    // Given
    stubPikachu(60, 0);

    // When
    EntityExchangeResult<Map<String, Object>> imported = sync("pikachu", ash);

    // Then
    assertThat(imported.getStatus().value()).isEqualTo(201);
    String location = imported.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
    assertThat(location).startsWith("/api/v1/local-pokemon/");
    Map<String, Object> body = imported.getResponseBody();
    assertThat(body).containsEntry("pokeapiId", 25).containsEntry("createdBy", "ash");

    Map<String, Object> fetched =
        client
            .get()
            .uri(location)
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .valueEquals(HttpHeaders.ETAG, "\"0\"")
            .expectBody(JSON)
            .returnResult()
            .getResponseBody();
    assertThat(fetched).isEqualTo(body);
  }

  @Test
  void should_refreshUpstreamButKeepProprietaryData_when_resynced() {
    // Given an imported Pikachu that the business has since enriched
    stubPikachu(60, 0);
    sync("pikachu", ash);
    LocalPokemon stored = repository.findByPokeapiId(new PokemonId(25)).orElseThrow();
    repository.save(
        new LocalPokemon(
            stored.id(),
            stored.pokeapiId(),
            stored.upstream(),
            new ProprietaryData(
                LocalizedNames.of(Map.of("pt-BR", "Pikachu")),
                "Kanto",
                "Viridian Forest",
                InternalTags.of(List.of("starter-line")),
                "Ash's partner"),
            stored.audit(),
            stored.version()));

    // When PokeAPI reports a new weight and misty re-syncs
    stubPikachu(61, 0);
    EntityExchangeResult<Map<String, Object>> resynced = sync("25", misty);

    // Then
    assertThat(resynced.getStatus().value()).isEqualTo(200);
    assertThat(resynced.getResponseHeaders().getFirst(HttpHeaders.ETAG)).isEqualTo("\"2\"");
    Map<String, Object> body = resynced.getResponseBody();
    assertThat(body)
        .containsEntry("weight", 6.1)
        .containsEntry("region", "Kanto")
        .containsEntry("habitatNotes", "Viridian Forest")
        .containsEntry("internalTags", List.of("starter-line"))
        .containsEntry("localizedNames", Map.of("pt-BR", "Pikachu"))
        .containsEntry("createdBy", "ash");
    assertThat((String) body.get("syncedAt")).isGreaterThan((String) body.get("createdAt"));
  }

  @Test
  void should_readPokeApiFreshAndRefreshTheCatalog_when_resyncedAfterAChange() {
    // Given Pikachu's catalog detail is cached at 6.0 kg, and the local copy was imported
    stubPikachu(60, 0);
    assertThat(catalogWeight()).isEqualTo(6.0);
    sync("pikachu", ash);

    // When PokeAPI now reports 6.1 kg: the catalog still serves its cached copy...
    stubPikachu(61, 0);
    assertThat(catalogWeight()).isEqualTo(6.0);
    EntityExchangeResult<Map<String, Object>> resynced = sync("25", misty);

    // Then ...but a re-sync reads PokeAPI itself, and the catalog shows the new data too
    assertThat(resynced.getResponseBody()).containsEntry("weight", 6.1);
    assertThat(catalogWeight()).isEqualTo(6.1);
  }

  @Test
  void should_keepASingleRecord_when_twoFirstImportsRace() throws Exception {
    // Given PokeAPI is slow enough for both requests to miss the existing record
    stubPikachu(60, 300);
    CountDownLatch start = new CountDownLatch(1);

    // When
    CompletableFuture<Integer> first =
        CompletableFuture.supplyAsync(() -> awaitThenSync(start, ash));
    CompletableFuture<Integer> second =
        CompletableFuture.supplyAsync(() -> awaitThenSync(start, misty));
    start.countDown();

    // Then
    assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 200);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from local_pokemon where pokeapi_id = 25", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void should_leaveTheRecordUntouched_when_pokeApiFailsDuringResync() {
    // Given
    stubPikachu(60, 0);
    sync("pikachu", ash);
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/pikachu")).willReturn(aResponse().withStatus(500)));

    // When
    EntityExchangeResult<Map<String, Object>> failed = sync("pikachu", misty);

    // Then
    assertThat(failed.getStatus().value()).isEqualTo(503);
    assertThat(
            jdbc.queryForObject(
                "select version from local_pokemon where pokeapi_id = 25", Long.class))
        .isZero();
  }

  @Test
  void should_syncTheFirstCatalogEntries_when_bulkSyncing() {
    // Given
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon"))
            .willReturn(okJson(PokeApiFixtures.json("list-offset-0-limit-3.json"))));
    for (int id : new int[] {1, 2, 3}) {
      stubResource("/api/v2/pokemon/" + id, "pokemon-" + id + ".json", 0);
      stubResource("/api/v2/pokemon-species/" + id, "species-" + id + ".json", 0);
    }
    stubResource("/api/v2/evolution-chain/1", "evolution-chain-1.json", 0);

    // When / Then
    client
        .post()
        .uri("/api/v1/local-pokemon/sync?limit=3")
        .header(HttpHeaders.AUTHORIZATION, ash)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.created")
        .isEqualTo(3)
        .jsonPath("$.results[*].name")
        .isEqualTo(List.of("bulbasaur", "ivysaur", "venusaur"));
    assertThat(jdbc.queryForObject("select count(*) from local_pokemon", Integer.class))
        .isEqualTo(3);
  }

  private double catalogWeight() {
    Map<String, Object> detail =
        client
            .get()
            .uri("/api/v1/pokemon/25")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(JSON)
            .returnResult()
            .getResponseBody();
    return ((Number) detail.get("weight")).doubleValue();
  }

  private int awaitThenSync(CountDownLatch start, String bearer) {
    try {
      start.await();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(interrupted);
    }
    return sync("pikachu", bearer).getStatus().value();
  }

  private EntityExchangeResult<Map<String, Object>> sync(String idOrName, String bearer) {
    return client
        .post()
        .uri("/api/v1/local-pokemon/sync/{idOrName}", idOrName)
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .exchange()
        .expectBody(JSON)
        .returnResult();
  }

  private static void stubPikachu(int weightHectograms, int delayMillis) {
    String pokemon =
        PokeApiFixtures.json("pokemon-25.json")
            .replace("\"weight\": 60", "\"weight\": " + weightHectograms);
    for (String path : new String[] {"/api/v2/pokemon/pikachu", "/api/v2/pokemon/25"}) {
      POKEAPI.stubFor(
          get(urlPathEqualTo(path)).willReturn(okJson(pokemon).withFixedDelay(delayMillis)));
    }
    stubResource("/api/v2/pokemon-species/25", "species-25.json", 0);
    stubResource("/api/v2/evolution-chain/10", "evolution-chain-10.json", 0);
  }

  private static void stubResource(String path, String fixture, int delayMillis) {
    POKEAPI.stubFor(
        get(urlPathEqualTo(path))
            .willReturn(okJson(PokeApiFixtures.json(fixture)).withFixedDelay(delayMillis)));
  }
}
