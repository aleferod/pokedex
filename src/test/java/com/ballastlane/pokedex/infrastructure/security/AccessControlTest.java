package com.ballastlane.pokedex.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.support.TestTokens;
import com.ballastlane.pokedex.support.WebSliceTest;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pins every access rule of the API. Not-yet-built routes answer 404 once access control lets the
 * request through, so 404 here means "not rejected for missing or insufficient credentials".
 */
@WebSliceTest
class AccessControlTest {

  private static final String LOCAL_POKEMON = "/api/v1/local-pokemon";

  @Autowired private MockMvcTester mvc;
  @Autowired private TokenPort tokenPort;
  @Autowired private JwtEncoder jwtEncoder;

  private TestTokens tokens;

  @BeforeEach
  void setUp() {
    tokens = new TestTokens(tokenPort, jwtEncoder);
  }

  @Test
  void should_letAnonymousCallersThrough_when_routeIsPublic() {
    assertThat(mvc.get().uri("/api/v1/pokemon").exchange()).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.get().uri("/api/v1/pokemon/pikachu").exchange()).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void should_keepAnswering404_when_anonymousCallerHitsAnUnknownRoute() {
    assertThat(mvc.get().uri("/api/v1/does-not-exist").exchange()).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void should_return401ProblemWithBearerChallenge_when_anonymousCallerHitsLocalReplica() {
    MvcTestResult result = mvc.get().uri(LOCAL_POKEMON).exchange();

    assertThat(result)
        .hasStatus(HttpStatus.UNAUTHORIZED)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .containsHeader(CorrelationIdFilter.HEADER);
    assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#unauthorized");
    assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo(LOCAL_POKEMON);
  }

  @Test
  void should_letUserThrough_when_readingTheLocalReplica() {
    MvcTestResult result =
        mvc.get()
            .uri(LOCAL_POKEMON)
            .header(HttpHeaders.AUTHORIZATION, tokens.bearerFor(Role.USER))
            .exchange();

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND).doesNotContainHeader(HttpHeaders.SET_COOKIE);
  }

  @Test
  void should_return403Problem_when_userTriesToDelete() {
    MvcTestResult result =
        mvc.delete()
            .uri(LOCAL_POKEMON + "/1")
            .header(HttpHeaders.AUTHORIZATION, tokens.bearerFor(Role.USER))
            .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.FORBIDDEN)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#forbidden");
  }

  @Test
  void should_letAdminDeleteWithoutCsrfToken_when_tokenHasAdminRole() {
    MvcTestResult result =
        mvc.delete()
            .uri(LOCAL_POKEMON + "/1")
            .header(HttpHeaders.AUTHORIZATION, tokens.bearerFor(Role.ADMIN))
            .exchange();

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void should_return401InvalidToken_when_tokenHasExpired() {
    assertInvalidToken(() -> tokens.expired(Role.USER));
  }

  @Test
  void should_return401InvalidToken_when_payloadWasTampered() {
    assertInvalidToken(tokens::tamperedToAdmin);
  }

  @Test
  void should_return401InvalidToken_when_tokenComesFromAnotherIssuer() {
    assertInvalidToken(() -> tokens.fromForeignIssuer(Role.ADMIN));
  }

  @Test
  void should_return401InvalidToken_when_tokenIsSignedWithAnotherKey() {
    assertInvalidToken(() -> tokens.signedWithForeignKey(Role.ADMIN));
  }

  @Test
  void should_return401InvalidToken_when_tokenIsNotAJwt() {
    assertInvalidToken(() -> "Bearer not-a-jwt");
  }

  private void assertInvalidToken(Supplier<String> authorization) {
    MvcTestResult result =
        mvc.get()
            .uri(LOCAL_POKEMON)
            .header(HttpHeaders.AUTHORIZATION, authorization.get())
            .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.UNAUTHORIZED)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
        .contains("error=\"invalid_token\"");
  }
}
