package com.ballastlane.pokedex.adapter.out.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.ballastlane.pokedex.support.IntegrationTest;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The circuit breaker end to end, with tight settings: once PokeAPI has failed enough, requests are
 * answered 503 without reaching it; after the open period, a recovered PokeAPI is used again.
 */
@IntegrationTest
@TestPropertySource(
    properties = {
      "app.pokeapi.circuit-breaker.minimum-calls=3",
      "app.pokeapi.circuit-breaker.sliding-window=3",
      "app.pokeapi.circuit-breaker.open-duration=PT1S"
    })
class PokeApiResilienceIT {

  @RegisterExtension
  static final WireMockExtension POKEAPI =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired private RestTestClient client;

  @DynamicPropertySource
  static void pokeApiProperties(DynamicPropertyRegistry registry) {
    registry.add("app.pokeapi.base-url", () -> POKEAPI.baseUrl() + "/api/v2");
  }

  @Test
  void should_failFastWhilePokeApiIsDown_and_useItAgainOnceItRecovers() throws Exception {
    // Given PokeAPI fails every request: one detail request makes three failed attempts
    POKEAPI.stubFor(get(urlPathMatching("/api/v2/.*")).willReturn(aResponse().withStatus(500)));
    client.get().uri("/api/v1/pokemon/1").exchange().expectStatus().isEqualTo(503);

    // When the circuit is open, the next request never reaches PokeAPI
    POKEAPI.resetRequests();
    client
        .get()
        .uri("/api/v1/pokemon/2")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectHeader()
        .exists(HttpHeaders.RETRY_AFTER);
    POKEAPI.verify(0, anyRequestedFor(anyUrl()));

    // Then, once the open period has passed and PokeAPI has recovered, it is used again
    POKEAPI.resetAll();
    stubBulbasaur();
    Thread.sleep(1_500);
    client
        .get()
        .uri("/api/v1/pokemon/1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("bulbasaur");
  }

  private static void stubBulbasaur() {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/1"))
            .willReturn(okJson(PokeApiFixtures.json("pokemon-1.json"))));
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon-species/1"))
            .willReturn(okJson(PokeApiFixtures.json("species-1.json"))));
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/evolution-chain/1"))
            .willReturn(okJson(PokeApiFixtures.json("evolution-chain-1.json"))));
  }
}
