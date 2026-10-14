package com.ballastlane.pokedex.adapter.in.web.pokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiFixtures;
import com.ballastlane.pokedex.support.IntegrationTest;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
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
}
