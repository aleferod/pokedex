package com.ballastlane.pokedex.adapter.in.web.pokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiFixtures;
import com.ballastlane.pokedex.support.IntegrationTest;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/** US02 end to end: HTTP -> controller -> use case -> PokeAPI adapter -> (WireMock) PokeAPI. */
@IntegrationTest
class PokemonDetailIT {

  @RegisterExtension
  static final WireMockExtension POKEAPI =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired private RestTestClient client;

  @DynamicPropertySource
  static void pokeApiProperties(DynamicPropertyRegistry registry) {
    registry.add("app.pokeapi.base-url", () -> POKEAPI.baseUrl() + "/api/v2");
  }

  @Test
  void should_returnEeveeWithItsEightBranches_when_requestedByName() {
    stub("eevee", 133, 133, 67);

    client
        .get()
        .uri("/api/v1/pokemon/eevee")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(133)
        .jsonPath("$.category")
        .isEqualTo("Evolution Pokémon")
        .jsonPath("$.imageUrl")
        .value(String.class, url -> assertThat(url).endsWith("/official-artwork/133.png"))
        .jsonPath("$.evolutionChain.name")
        .isEqualTo("eevee")
        .jsonPath("$.evolutionChain.evolvesTo[*].name")
        .isEqualTo(
            List.of(
                "vaporeon",
                "jolteon",
                "flareon",
                "espeon",
                "umbreon",
                "leafeon",
                "glaceon",
                "sylveon"))
        .jsonPath("$.evolutionChain.evolvesTo[0].condition")
        .isEqualTo("Use Water Stone")
        .jsonPath("$.evolutionChain.evolvesTo[3].condition")
        .isEqualTo("Level up with high friendship during the day")
        .jsonPath("$.evolutionChain.evolvesTo[5].condition")
        .isEqualTo("Use Leaf Stone");
  }

  @Test
  void should_matchNamesCaseInsensitively_when_requestedInUpperCase() {
    stub("pikachu", 25, 25, 10);

    client
        .get()
        .uri("/api/v1/pokemon/PIKACHU")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(25)
        .jsonPath("$.evolutionChain.name")
        .isEqualTo("pichu")
        .jsonPath("$.evolutionChain.evolvesTo[0].evolvesTo[0].condition")
        .isEqualTo("Use Thunder Stone");
  }

  @Test
  void should_answer404_when_pokeApiDoesNotKnowThePokemon() {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/missingno")).willReturn(aResponse().withStatus(404)));

    client
        .get()
        .uri("/api/v1/pokemon/missingno")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.detail")
        .isEqualTo("No Pokémon matches 'missingno'.");
  }

  private static void stub(String pathSegment, int pokemon, int species, int chain) {
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
}
