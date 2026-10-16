package com.ballastlane.pokedex.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.PokedexApplication;
import com.ballastlane.pokedex.support.PostgresTestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(PostgresTestcontainersConfiguration.class)
class SchemaMigrationIT {

  @Autowired private Flyway flyway;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PostgreSQLContainer postgres;

  @Test
  void should_applyAndRecordEveryMigration_when_databaseStartsEmpty() {
    // When the application has started against a fresh database
    MigrationInfo[] applied = flyway.info().applied();

    // Then
    assertThat(applied)
        .extracting(info -> info.getVersion().getVersion(), MigrationInfo::getState)
        .first()
        .satisfies(
            first -> {
              assertThat(first.toList()).containsExactly("1", MigrationState.SUCCESS);
            });
    assertThat(flyway.info().pending()).isEmpty();
  }

  @Test
  void should_runNoMigration_when_serviceRestartsOnMigratedDatabase() {
    // Given
    List<Map<String, Object>> historyBefore = history();
    assertThat(historyBefore).isNotEmpty();

    // When a second instance starts against the same, already migrated database
    try (ConfigurableApplicationContext restarted =
        new SpringApplicationBuilder(PokedexApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .properties(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword())
            .run()) {

      // Then
      assertThat(restarted.isRunning()).isTrue();
      assertThat(history()).isEqualTo(historyBefore);
    }
  }

  private List<Map<String, Object>> history() {
    return jdbc.queryForList(
        "select installed_rank, version, checksum, installed_on, success"
            + " from flyway_schema_history order by installed_rank");
  }
}
