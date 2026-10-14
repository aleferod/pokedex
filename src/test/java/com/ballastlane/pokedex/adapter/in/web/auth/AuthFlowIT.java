package com.ballastlane.pokedex.adapter.in.web.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/** register -> login -> me -> protected route, against the real stack and PostgreSQL. */
@IntegrationTest
class AuthFlowIT {

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void should_registerLoginAndReachProtectedRoutes_when_followingTheHappyPath() {
    // Given a new account
    client
        .post()
        .uri("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("username", "Gary", "email", "gary@pallet.town", "password", "Eevee@133"))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .doesNotExist(HttpHeaders.SET_COOKIE)
        .expectBody()
        .jsonPath("$.username")
        .isEqualTo("gary")
        .jsonPath("$.role")
        .isEqualTo("USER");

    // When it logs in (username is case-insensitive)
    Map<String, Object> login =
        client
            .post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("username", "GARY", "password", "Eevee@133"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .doesNotExist(HttpHeaders.SET_COOKIE)
            .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
            .returnResult()
            .getResponseBody();
    assertThat(login).isNotNull();
    String bearer = "Bearer " + login.get("accessToken");
    assertThat(Instant.parse((String) login.get("expiresAt")))
        .isBetween(
            Instant.now().plus(Duration.ofMinutes(59)), Instant.now().plus(Duration.ofMinutes(61)));

    // Then the token identifies the account
    client
        .get()
        .uri("/api/v1/auth/me")
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.username")
        .isEqualTo("gary");

    // And opens the protected area (no endpoint yet, so access control lets it through to 404)
    client
        .get()
        .uri("/api/v1/local-pokemon")
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .doesNotExist(HttpHeaders.SET_COOKIE);

    // And the password is stored only as a BCrypt hash
    String stored =
        jdbc.queryForObject(
            "select password_hash from users where username = 'gary'", String.class);
    assertThat(stored).startsWith("$2").isNotEqualTo("Eevee@133");
  }

  @Test
  void should_return409_when_registeringAnExistingUsernameInAnotherCase() {
    Map<String, String> misty =
        Map.of("username", "misty", "email", "misty@cerulean.city", "password", "Starmie@121");
    client
        .post()
        .uri("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .body(misty)
        .exchange()
        .expectStatus()
        .isCreated();

    client
        .post()
        .uri("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Map.of("username", "MISTY", "email", "other@cerulean.city", "password", "Starmie@121"))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.errors[0].field")
        .isEqualTo("username");
  }

  @Test
  void should_return401_when_loggingInWithAWrongPassword() {
    client
        .post()
        .uri("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("username", "nobody-here", "password", "Wrong@1234"))
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.detail")
        .isEqualTo("Invalid username or password.");
  }
}
