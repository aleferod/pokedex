package com.ballastlane.pokedex.adapter.in.web.pokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiFixtures;
import com.ballastlane.pokedex.support.IntegrationTest;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/** US01 end to end: HTTP -> controller -> use case -> PokeAPI adapter -> (WireMock) PokeAPI. */
@IntegrationTest
class PokemonCatalogIT {

  @RegisterExtension
  static final WireMockExtension POKEAPI =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired private RestTestClient client;

  @DynamicPropertySource
  static void pokeApiProperties(DynamicPropertyRegistry registry) {
    registry.add("app.pokeapi.base-url", () -> POKEAPI.baseUrl() + "/api/v2");
  }

  @Test
  void should_serveTheFirstPageFromPokeApi_when_anonymousClientBrowses() {
    // Given
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon"))
            .willReturn(okJson(PokeApiFixtures.json("list-offset-0-limit-3.json"))));
    for (int id : new int[] {1, 2, 3}) {
      POKEAPI.stubFor(
          get(urlPathEqualTo("/api/v2/pokemon/" + id))
              .willReturn(okJson(PokeApiFixtures.json("pokemon-" + id + ".json"))));
      POKEAPI.stubFor(
          get(urlPathEqualTo("/api/v2/pokemon-species/" + id))
              .willReturn(okJson(PokeApiFixtures.json("species-" + id + ".json"))));
    }

    // When / Then
    client
        .get()
        .uri("/api/v1/pokemon?page=0&size=3")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content[*].name")
        .isEqualTo(List.of("bulbasaur", "ivysaur", "venusaur"))
        .jsonPath("$.content[0].category")
        .isEqualTo("Seed Pokémon")
        .jsonPath("$.content[0].weight")
        .isEqualTo(6.9)
        .jsonPath("$.content[2].weight")
        .isEqualTo(100.0)
        .jsonPath("$.totalElements")
        .isEqualTo(1351)
        .jsonPath("$.totalPages")
        .isEqualTo(451);
  }

  @Test
  void should_serveTheSecondVisitFromTheCache_when_aPageIsRequestedAgain() {
    // Given
    stubFirstThree(0);

    // When
    String first = page();
    String second = page();

    // Then
    assertThat(second).isEqualTo(first);
    POKEAPI.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon")));
    for (int id : new int[] {1, 2, 3}) {
      POKEAPI.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon/" + id)));
      POKEAPI.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon-species/" + id)));
    }
  }

  @Test
  void should_askPokeApiAgain_when_theEarlierAttemptFailed() {
    // Given an outage...
    POKEAPI.stubFor(get(urlPathEqualTo("/api/v2/pokemon")).willReturn(aResponse().withStatus(500)));
    client.get().uri("/api/v1/pokemon?page=0&size=3").exchange().expectStatus().isEqualTo(503);

    // ...after which PokeAPI recovers
    POKEAPI.resetAll();
    stubFirstThree(0);

    // When / Then: the failure was not remembered
    assertThat(page()).contains("bulbasaur");
    POKEAPI.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon")));
  }

  @Test
  void should_loadThePageOnce_when_twoFirstVisitsArriveTogether() throws Exception {
    // Given a slow PokeAPI, so the second visit arrives while the first is still loading
    stubFirstThree(500);
    CountDownLatch start = new CountDownLatch(1);

    // When
    CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> awaitThenPage(start));
    CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> awaitThenPage(start));
    start.countDown();

    // Then
    assertThat(first.get()).isEqualTo(second.get()).contains("bulbasaur");
    POKEAPI.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon")));
  }

  @Test
  void should_answer503WithRetryAfter_when_pokeApiConnectionFails() {
    // Given
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    // When / Then
    client
        .get()
        .uri("/api/v1/pokemon")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectHeader()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .expectHeader()
        .exists(HttpHeaders.RETRY_AFTER)
        .expectBody()
        .jsonPath("$.type")
        .value(String.class, type -> assertThat(type).endsWith("#service-unavailable"));
  }

  private String page() {
    return client
        .get()
        .uri("/api/v1/pokemon?page=0&size=3")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();
  }

  private String awaitThenPage(CountDownLatch start) {
    try {
      start.await();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(interrupted);
    }
    return page();
  }

  private static void stubFirstThree(int listDelayMillis) {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon"))
            .willReturn(
                okJson(PokeApiFixtures.json("list-offset-0-limit-3.json"))
                    .withFixedDelay(listDelayMillis)));
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
