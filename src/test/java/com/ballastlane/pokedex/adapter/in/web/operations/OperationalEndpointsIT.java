package com.ballastlane.pokedex.adapter.in.web.operations;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import com.ballastlane.pokedex.support.IntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class OperationalEndpointsIT {

  @Autowired private RestTestClient client;
  @Autowired private BuildProperties build;
  @LocalServerPort private int port;

  @Test
  void should_reportUpWithoutDetails_when_databaseIsReachable() {
    client
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists(CorrelationIdFilter.HEADER)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP")
        .jsonPath("$.components")
        .doesNotExist()
        .jsonPath("$.details")
        .doesNotExist();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/actuator/health/liveness", "/actuator/health/readiness"})
  void should_reportUp_when_probeIsCalled(String probe) {
    client
        .get()
        .uri(probe)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP");
  }

  @Test
  void should_exposeNameAndVersionOnly_when_infoIsCalled() {
    client
        .get()
        .uri("/actuator/info")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.build.name")
        .isEqualTo("pokedex-api")
        .jsonPath("$.build.version")
        .isEqualTo(build.getVersion())
        .jsonPath("$.env")
        .doesNotExist();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"/actuator/env", "/actuator/beans", "/actuator/configprops", "/actuator/heapdump"})
  void should_return404ProblemDetail_when_sensitiveEndpointIsRequested(String endpoint) {
    client
        .get()
        .uri(endpoint)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void should_return404_when_shutdownIsPosted() {
    client.post().uri("/actuator/shutdown").exchange().expectStatus().isNotFound();
  }

  @Test
  void should_return405ProblemDetailWithAllow_when_healthIsDeleted() {
    client
        .delete()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isEqualTo(405)
        .expectHeader()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .expectHeader()
        .exists(HttpHeaders.ALLOW);
  }

  @Test
  void should_publishOpenApiDocument_when_apiDocsAreRequested() {
    client
        .get()
        .uri("/v3/api-docs")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.openapi")
        .value(String.class, version -> assertThat(version).startsWith("3."))
        .jsonPath("$.info.title")
        .isEqualTo("Pokédex API")
        .jsonPath("$.info.version")
        .isEqualTo(build.getVersion())
        .jsonPath("$.info.license.name")
        .isEqualTo("MIT");
  }

  @Test
  void should_serveSwaggerUi_when_swaggerUiHtmlIsOpened() throws Exception {
    // Given a browser-like client that follows redirects
    try (HttpClient browser =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {

      // When
      HttpResponse<String> page =
          browser.send(
              HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/swagger-ui.html"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());

      // Then
      assertThat(page.statusCode()).isEqualTo(200);
      assertThat(page.body()).containsIgnoringCase("swagger-ui");
    }
  }
}
