package com.ballastlane.pokedex.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.PokedexApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Checks how application.yml and the profile files resolve, without starting the application or a
 * database. The host's environment variables are removed so a developer's shell cannot hide a
 * missing setting.
 */
class ProfileConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(
              context ->
                  context
                      .getEnvironment()
                      .getPropertySources()
                      .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
          .withInitializer(new ConfigDataApplicationContextInitializer());

  @Test
  void should_useLocalProfile_when_noProfileIsActive() {
    runner.run(
        context -> {
          ConfigurableEnvironment environment = context.getEnvironment();
          assertThat(environment.getActiveProfiles()).isEmpty();
          assertThat(environment.getDefaultProfiles()).containsExactly("local");
          // Value may come from a developer's .env (imported by the local profile), so only the
          // driver is asserted here; the docker test pins the exact default URL.
          assertThat(environment.getProperty("spring.datasource.url"))
              .startsWith("jdbc:postgresql://");
          assertThat(environment.getPropertySources().stream().map(PropertySource::getName))
              .anySatisfy(name -> assertThat(name).contains("application-local.yml"));
        });
  }

  @Test
  void should_targetComposeDatabaseOnPort8080_when_dockerProfileIsActive() {
    runner
        .withPropertyValues("spring.profiles.active=docker", "DB_PASSWORD=placeholder")
        .run(
            context -> {
              assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                  .isEqualTo("jdbc:postgresql://postgres:5432/pokedex");
              assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8080");
            });
  }

  @Test
  void should_failStartupNamingTheVariable_when_dockerProfileLacksDbPassword() {
    // Given an environment without host variables (so no DB_PASSWORD) and the docker profile
    StandardEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    SpringApplicationBuilder application =
        new SpringApplicationBuilder(PokedexApplication.class)
            .environment(environment)
            .web(WebApplicationType.NONE)
            .profiles("docker")
            .logStartupInfo(false);

    // When / Then: fails while preparing the environment, before any bean or database is touched
    assertThatThrownBy(application::run)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DB_PASSWORD")
        .hasMessageContaining(".env.example");
  }

  @ParameterizedTest
  @ValueSource(strings = {"local", "docker", "test"})
  void should_onlyValidateSchema_when_anyProfileIsActive(String profile) {
    runner
        .withPropertyValues("spring.profiles.active=" + profile, "DB_PASSWORD=placeholder")
        .run(
            context ->
                assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate"));
  }
}
