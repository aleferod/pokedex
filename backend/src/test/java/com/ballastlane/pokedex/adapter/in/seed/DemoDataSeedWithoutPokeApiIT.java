package com.ballastlane.pokedex.adapter.in.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.support.IntegrationTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Demo data when PokeAPI cannot be reached at first start: the service still comes up usable. */
@IntegrationTest
@TestPropertySource(
    properties = {"app.seed.enabled=true", "app.seed.location=classpath:seed/test-demo-data.json"})
class DemoDataSeedWithoutPokeApiIT {

  private static final ParameterizedTypeReference<Map<String, Object>> JSON =
      new ParameterizedTypeReference<>() {};

  @Autowired private RestTestClient client;

  @DynamicPropertySource
  static void unreachablePokeApi(DynamicPropertyRegistry registry) {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
    registry.add("app.pokeapi.base-url", () -> "http://localhost:" + closedPort + "/api/v2");
  }

  @Test
  void should_startWithTheAccountsButNoLocalRecords_when_pokeApiIsDownAtFirstStart() {
    Map<String, Object> token =
        client
            .post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("username", "admin", "password", "Admin@123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(JSON)
            .returnResult()
            .getResponseBody();

    client
        .get()
        .uri("/api/v1/local-pokemon")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get("accessToken"))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.totalElements")
        .isEqualTo(0);
    assertThat(token).containsKey("accessToken");
  }
}
