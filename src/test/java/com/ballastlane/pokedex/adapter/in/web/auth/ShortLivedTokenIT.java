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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/** JWT_TTL is honoured: a token stops working once its lifetime has passed. */
@IntegrationTest
@TestPropertySource(properties = {"app.security.jwt.ttl=PT1S", "app.security.jwt.clock-skew=PT0S"})
class ShortLivedTokenIT {

  @Autowired private RestTestClient client;

  @Test
  void should_rejectAsInvalidToken_when_tokenLifetimeHasPassed() throws InterruptedException {
    // Given an account and a token that lives one second
    client
        .post()
        .uri("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("username", "brock", "email", "brock@pewter.city", "password", "Onix@0095"))
        .exchange()
        .expectStatus()
        .isCreated();
    Map<String, Object> login =
        client
            .post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("username", "brock", "password", "Onix@0095"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
            .returnResult()
            .getResponseBody();
    assertThat(login).isNotNull();
    Instant expiresAt = Instant.parse((String) login.get("expiresAt"));
    assertThat(Duration.between(Instant.now(), expiresAt))
        .isLessThanOrEqualTo(Duration.ofSeconds(1));

    // When the lifetime has passed
    Thread.sleep(Duration.ofMillis(2_100));

    // Then
    client
        .get()
        .uri("/api/v1/auth/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.get("accessToken"))
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .value(
            HttpHeaders.WWW_AUTHENTICATE,
            challenge -> assertThat(challenge).contains("error=\"invalid_token\""));
  }
}
