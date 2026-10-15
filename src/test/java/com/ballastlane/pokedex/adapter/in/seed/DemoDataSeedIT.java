package com.ballastlane.pokedex.adapter.in.seed;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiFixtures;
import com.ballastlane.pokedex.support.IntegrationTest;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Demo data end to end: seeded while the application starts (WireMock serves PokeAPI), then used
 * the way the demo script uses it. A second run stands in for a restart.
 */
@IntegrationTest
@TestPropertySource(
    properties = {"app.seed.enabled=true", "app.seed.location=classpath:seed/test-demo-data.json"})
class DemoDataSeedIT {

  private static final ParameterizedTypeReference<Map<String, Object>> JSON =
      new ParameterizedTypeReference<>() {};

  @RegisterExtension
  static final WireMockExtension POKEAPI =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired private RestTestClient client;
  @Autowired private DemoDataSeeder seeder;

  @DynamicPropertySource
  static void pokeApiProperties(DynamicPropertyRegistry registry) {
    registry.add("app.pokeapi.base-url", () -> POKEAPI.baseUrl() + "/api/v2");
  }

  /** Runs before the application (and so the seeding) starts. */
  @BeforeAll
  static void pokeApiServesTheSeededPokemon() {
    stub(1, 1, 1);
    stub(2, 2, 1);
    stub(3, 3, 1);
    stub(25, 25, 10);
  }

  @Test
  void should_offerTheDemoAccountsAndAPopulatedReplica_and_keepEditsWhenSeededAgain() {
    // The demo accounts can log in, with their roles
    String trainer = login("trainer", "Trainer@123");
    String admin = login("admin", "Admin@123");
    assertThat(me(trainer)).containsEntry("role", "USER");
    assertThat(me(admin)).containsEntry("role", "ADMIN");

    // The replica is populated with the seed file's proprietary fields and fresh PokeAPI data
    Map<String, Object> all = read("/api/v1/local-pokemon", trainer);
    assertThat(all).containsEntry("totalElements", 4);
    Map<String, Object> starters = read("/api/v1/local-pokemon?tag=starter", trainer);
    assertThat(names(starters)).containsExactly("bulbasaur");
    Map<String, Object> bulbasaur = content(starters).getFirst();
    assertThat(bulbasaur)
        .containsEntry("category", "Seed Pokémon")
        .containsEntry("region", "Kanto")
        .containsEntry("createdBy", "admin")
        .containsEntry(
            "localizedNames", Map.of("ja", "フシギダネ", "fr", "Bulbizarre", "de", "Bisasam"));

    // The trainer edits Bulbasaur; seeding again (a restart) keeps it and duplicates nothing
    String location = "/api/v1/local-pokemon/" + bulbasaur.get("id");
    client
        .patch()
        .uri(location)
        .header(HttpHeaders.AUTHORIZATION, trainer)
        .header(HttpHeaders.IF_MATCH, "\"" + bulbasaur.get("version") + "\"")
        .contentType(MediaType.parseMediaType("application/merge-patch+json"))
        .body("{\"notes\": \"Ash's favourite\"}")
        .exchange()
        .expectStatus()
        .isOk();
    POKEAPI.resetRequests();

    DemoDataSeeder.Result again = seeder.seed();

    assertThat(again).isEqualTo(new DemoDataSeeder.Result(0, 2, 0, 0, 4, 0));
    POKEAPI.verify(0, anyRequestedFor(anyUrl()));
    assertThat(read(location, trainer)).containsEntry("notes", "Ash's favourite");
    assertThat(read("/api/v1/local-pokemon", trainer)).containsEntry("totalElements", 4);

    // The admin can delete; the trainer cannot
    client
        .delete()
        .uri(location)
        .header(HttpHeaders.AUTHORIZATION, trainer)
        .exchange()
        .expectStatus()
        .isForbidden();
    client
        .delete()
        .uri(location)
        .header(HttpHeaders.AUTHORIZATION, admin)
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  private String login(String username, String password) {
    Map<String, Object> token =
        client
            .post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("username", username, "password", password))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(JSON)
            .returnResult()
            .getResponseBody();
    return "Bearer " + token.get("accessToken");
  }

  private Map<String, Object> me(String bearer) {
    return read("/api/v1/auth/me", bearer);
  }

  private Map<String, Object> read(String uri, String bearer) {
    return client
        .get()
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(JSON)
        .returnResult()
        .getResponseBody();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> content(Map<String, Object> page) {
    return (List<Map<String, Object>>) page.get("content");
  }

  private static List<Object> names(Map<String, Object> page) {
    return content(page).stream().map(entry -> entry.get("name")).toList();
  }

  private static void stub(int number, int species, int chain) {
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon/" + number))
            .willReturn(okJson(PokeApiFixtures.json("pokemon-" + number + ".json"))));
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/pokemon-species/" + species))
            .willReturn(okJson(PokeApiFixtures.json("species-" + species + ".json"))));
    POKEAPI.stubFor(
        get(urlPathEqualTo("/api/v2/evolution-chain/" + chain))
            .willReturn(okJson(PokeApiFixtures.json("evolution-chain-" + chain + ".json"))));
  }
}
