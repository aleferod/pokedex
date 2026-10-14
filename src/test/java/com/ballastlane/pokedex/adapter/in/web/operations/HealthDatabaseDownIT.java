package com.ballastlane.pokedex.adapter.in.web.operations;

import com.ballastlane.pokedex.support.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Uses its own database container, because the test stops it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@DirtiesContext
class HealthDatabaseDownIT {

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(PostgresTestcontainersConfiguration.POSTGRES_IMAGE);

  @Autowired private RestTestClient client;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    POSTGRES.start();
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    // Fail fast once the database is gone instead of waiting for Hikari's 30 s default.
    registry.add("spring.datasource.hikari.connection-timeout", () -> "2000");
  }

  @AfterAll
  static void stopDatabase() {
    POSTGRES.stop();
  }

  @Test
  void should_reportDown503_when_databaseBecomesUnreachable() {
    // Given a healthy service
    client.get().uri("/actuator/health").exchange().expectStatus().isOk();

    // When the database goes away
    POSTGRES.stop();

    // Then
    client
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("DOWN")
        .jsonPath("$.components")
        .doesNotExist();
  }
}
