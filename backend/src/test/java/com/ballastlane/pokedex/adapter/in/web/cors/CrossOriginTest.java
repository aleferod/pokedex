package com.ballastlane.pokedex.adapter.in.web.cors;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import com.ballastlane.pokedex.support.WebSliceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebSliceTest
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class CrossOriginTest {

  private static final String ALLOWED = "http://localhost:5173";

  @Autowired private MockMvcTester mvc;

  @Test
  void should_allowPreflight_when_originIsConfigured() {
    // When
    MvcTestResult result =
        mvc.options()
            .uri("/api/v1/pokemon")
            .header(HttpHeaders.ORIGIN, ALLOWED)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.OK)
        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED);
  }

  @Test
  void should_rejectPreflightWithProblemDetail_when_originIsNotConfigured() {
    // When
    MvcTestResult result =
        mvc.options()
            .uri("/api/v1/pokemon")
            .header(HttpHeaders.ORIGIN, "https://evil.example")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.FORBIDDEN)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
        .containsHeader(CorrelationIdFilter.HEADER);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#forbidden");
    assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo("/api/v1/pokemon");
    assertThat(result)
        .bodyJson()
        .extractingPath("$")
        .asMap()
        .containsOnlyKeys("type", "title", "status", "detail", "instance", "timestamp", "errors");
  }

  @Test
  void should_advertiseMethodsAndHeaders_when_preflightAsksForPatchWithAuthorization() {
    // When
    MvcTestResult result =
        mvc.options()
            .uri("/api/v1/local-pokemon/1")
            .header(HttpHeaders.ORIGIN, ALLOWED)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, If-Match")
            .exchange();

    // Then
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
        .contains("PATCH");
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
        .containsIgnoringCase("Authorization")
        .containsIgnoringCase("If-Match");
  }

  @Test
  void should_exposeLocationEtagAndCorrelationId_when_simpleCrossOriginRequest() {
    // When
    MvcTestResult result =
        mvc.get().uri("/api/v1/anything").header(HttpHeaders.ORIGIN, ALLOWED).exchange();

    // Then
    assertThat(result).hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED);
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
        .containsIgnoringCase("Location")
        .containsIgnoringCase("ETag")
        .containsIgnoringCase(CorrelationIdFilter.HEADER);
  }
}
