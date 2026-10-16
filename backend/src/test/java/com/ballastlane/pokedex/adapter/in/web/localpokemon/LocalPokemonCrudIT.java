package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.pokeapi.PokeApiFixtures;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.support.IntegrationTest;
import com.ballastlane.pokedex.support.TestTokens;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * US04 end to end: create, list, patch, replace and delete through HTTP into PostgreSQL, with
 * WireMock standing in for PokeAPI. ETags travel exactly as a client would send them back.
 */
@IntegrationTest
class LocalPokemonCrudIT {

  private static final ParameterizedTypeReference<Map<String, Object>> JSON =
      new ParameterizedTypeReference<>() {};
  private static final MediaType MERGE_PATCH =
      MediaType.parseMediaType("application/merge-patch+json");

  @RegisterExtension
  static final WireMockExtension POKEAPI =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TokenPort tokenPort;
  @Autowired private JwtEncoder jwtEncoder;

  private String ash;
  private String misty;
  private String admin;

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
    admin = tokens.bearerFor(Role.ADMIN);
    stub(1, 1);
    stub(25, 10);
  }

  @Test
  void should_supportTheWholeLifecycle_when_usersEditConcurrently() {
    // Create Bulbasaur with proprietary data, and an untagged Pikachu
    EntityExchangeResult<Map<String, Object>> created =
        send(
            "POST",
            "/api/v1/local-pokemon",
            ash,
            null,
            MediaType.APPLICATION_JSON,
            """
            {"pokeapiId": 1, "region": "Kanto", "internalTags": ["starter"],
             "localizedNames": {"pt-BR": "Bulbassauro"}}
            """);
    assertThat(created.getStatus().value()).isEqualTo(201);
    assertThat(created.getResponseBody())
        .containsEntry("name", "bulbasaur")
        .containsEntry("region", "Kanto")
        .containsEntry("internalTags", List.of("starter"))
        .containsEntry("localizedNames", Map.of("pt-BR", "Bulbassauro"))
        .containsEntry("createdBy", "ash");
    String location = created.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
    String readByBoth = created.getResponseHeaders().getFirst(HttpHeaders.ETAG);
    assertThat(readByBoth).isEqualTo("\"0\"");
    send(
        "POST",
        "/api/v1/local-pokemon",
        ash,
        null,
        MediaType.APPLICATION_JSON,
        "{\"pokeapiId\": 25}");

    // List by tag, ignoring case: Bulbasaur only
    EntityExchangeResult<Map<String, Object>> starters =
        send("GET", "/api/v1/local-pokemon?tag=STARTER", misty, null, null, null);
    assertThat(starters.getResponseBody()).containsEntry("totalElements", 1);
    assertThat(starters.getResponseBody().get("content").toString()).contains("bulbasaur");

    // Ash patches the notes with the ETag both users read
    EntityExchangeResult<Map<String, Object>> patched =
        send("PATCH", location, ash, readByBoth, MERGE_PATCH, "{\"notes\": \"Loves sunlight\"}");
    assertThat(patched.getStatus().value()).isEqualTo(200);
    assertThat(patched.getResponseHeaders().getFirst(HttpHeaders.ETAG)).isEqualTo("\"1\"");
    assertThat(patched.getResponseBody())
        .containsEntry("notes", "Loves sunlight")
        .containsEntry("region", "Kanto");

    // Misty patches with the same, now stale, ETag: 409 and Ash's change survives
    EntityExchangeResult<Map<String, Object>> stale =
        send("PATCH", location, misty, readByBoth, MERGE_PATCH, "{\"notes\": \"Grumpy\"}");
    assertThat(stale.getStatus().value()).isEqualTo(409);
    assertThat((String) stale.getResponseBody().get("type")).endsWith("#conflict");
    assertThat(send("GET", location, misty, null, null, null).getResponseBody())
        .containsEntry("notes", "Loves sunlight");

    // Trying to change an upstream field is refused and changes nothing
    EntityExchangeResult<Map<String, Object>> readOnly =
        send("PATCH", location, misty, "\"1\"", MERGE_PATCH, "{\"weight\": 999, \"notes\": \"x\"}");
    assertThat(readOnly.getStatus().value()).isEqualTo(400);
    assertThat(readOnly.getResponseBody().get("errors").toString()).contains("weight", "read-only");

    // Misty reloads and replaces: omitted proprietary fields are cleared
    EntityExchangeResult<Map<String, Object>> replaced =
        send(
            "PUT", location, misty, "\"1\"", MediaType.APPLICATION_JSON, "{\"region\": \"Johto\"}");
    assertThat(replaced.getStatus().value()).isEqualTo(200);
    assertThat(replaced.getResponseHeaders().getFirst(HttpHeaders.ETAG)).isEqualTo("\"2\"");
    assertThat(replaced.getResponseBody())
        .containsEntry("region", "Johto")
        .containsEntry("notes", null)
        .containsEntry("internalTags", List.of())
        .containsEntry("name", "bulbasaur");

    // A USER cannot delete; an ADMIN can, after which the record is gone
    assertThat(send("DELETE", location, misty, null, null, null).getStatus().value())
        .isEqualTo(403);
    EntityExchangeResult<Map<String, Object>> deleted =
        send("DELETE", location, admin, "\"2\"", null, null);
    assertThat(deleted.getStatus().value()).isEqualTo(204);
    assertThat(deleted.getResponseBody()).isNull();
    assertThat(send("GET", location, ash, null, null, null).getStatus().value()).isEqualTo(404);
    assertThat(
            jdbc.queryForObject("select count(*) from local_pokemon_localized_name", Integer.class))
        .isZero();
  }

  @Test
  void should_return409NamingPokeapiId_when_theNumberIsAlreadyStored() {
    send(
        "POST",
        "/api/v1/local-pokemon",
        ash,
        null,
        MediaType.APPLICATION_JSON,
        "{\"pokeapiId\": 1}");

    EntityExchangeResult<Map<String, Object>> duplicate =
        send(
            "POST",
            "/api/v1/local-pokemon",
            misty,
            null,
            MediaType.APPLICATION_JSON,
            "{\"pokeapiId\": 1}");

    assertThat(duplicate.getStatus().value()).isEqualTo(409);
    assertThat(duplicate.getResponseBody().get("errors").toString()).contains("pokeapiId");
  }

  @Test
  void should_return400NamingPokeapiIdAndStoreNothing_when_pokeApiHasNoSuchNumber() {
    POKEAPI.stubFor(get(urlPathEqualTo("/api/v2/pokemon/99999")).willReturn(notFound()));

    EntityExchangeResult<Map<String, Object>> unknown =
        send(
            "POST",
            "/api/v1/local-pokemon",
            ash,
            null,
            MediaType.APPLICATION_JSON,
            "{\"pokeapiId\": 99999}");

    assertThat(unknown.getStatus().value()).isEqualTo(400);
    assertThat(unknown.getResponseBody().get("errors").toString())
        .contains("pokeapiId", "no Pokémon has this number");
    assertThat(jdbc.queryForObject("select count(*) from local_pokemon", Integer.class)).isZero();
  }

  @Test
  void should_return428AndChangeNothing_when_ifMatchIsMissing() {
    String location =
        send(
                "POST",
                "/api/v1/local-pokemon",
                ash,
                null,
                MediaType.APPLICATION_JSON,
                "{\"pokeapiId\": 25}")
            .getResponseHeaders()
            .getFirst(HttpHeaders.LOCATION);

    EntityExchangeResult<Map<String, Object>> blind =
        send("PUT", location, ash, null, MediaType.APPLICATION_JSON, "{\"region\": \"Johto\"}");

    assertThat(blind.getStatus().value()).isEqualTo(428);
    assertThat(jdbc.queryForObject("select version from local_pokemon", Long.class)).isZero();
  }

  private EntityExchangeResult<Map<String, Object>> send(
      String method, String uri, String bearer, String ifMatch, MediaType type, String body) {
    var request =
        client
            .method(HttpMethod.valueOf(method))
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, bearer);
    if (ifMatch != null) {
      request.header(HttpHeaders.IF_MATCH, ifMatch);
    }
    if (body != null) {
      request.contentType(type).body(body);
    }
    return request.exchange().expectBody(JSON).returnResult();
  }

  private static void stub(int number, int evolutionChain) {
    stubResource("/api/v2/pokemon/" + number, "pokemon-" + number + ".json");
    stubResource("/api/v2/pokemon-species/" + number, "species-" + number + ".json");
    stubResource(
        "/api/v2/evolution-chain/" + evolutionChain, "evolution-chain-" + evolutionChain + ".json");
  }

  private static void stubResource(String path, String fixture) {
    POKEAPI.stubFor(get(urlPathEqualTo(path)).willReturn(okJson(PokeApiFixtures.json(fixture))));
  }
}
