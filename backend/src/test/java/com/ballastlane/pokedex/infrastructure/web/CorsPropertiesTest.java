package com.ballastlane.pokedex.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CorsPropertiesTest {

  @Test
  void should_bindEveryOrigin_when_originsAreCommaSeparated() {
    // When
    CorsProperties properties = bind("http://localhost:5173, https://pokedex.example:8443");

    // Then
    assertThat(properties.allowedOrigins())
        .containsExactly("http://localhost:5173", "https://pokedex.example:8443");
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", "http://localhost:5173,*", "https://*.example.com"})
  void should_rejectConfiguration_when_wildcardOriginIsConfigured(String origins) {
    assertThatThrownBy(() -> bind(origins))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining("explicit origins are required");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "localhost:5173",
        "http://localhost:5173/app",
        "ftp://files.example",
        "http://a.example,,http://b.example"
      })
  void should_rejectConfiguration_when_originIsMalformedOrBlank(String origins) {
    assertThatThrownBy(() -> bind(origins))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining("scheme://host[:port]");
  }

  @Test
  void should_failStartup_when_wildcardOriginIsConfigured() {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesOnly.class)
        .withPropertyValues("app.cors.allowed-origins=*")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .rootCause()
                  .hasMessageContaining("explicit origins are required");
            });
  }

  private static CorsProperties bind(String origins) {
    Binder binder =
        new Binder(new MapConfigurationPropertySource(Map.of("app.cors.allowed-origins", origins)));
    return binder.bind("app.cors", CorsProperties.class).get();
  }

  @EnableConfigurationProperties(CorsProperties.class)
  static class PropertiesOnly {}
}
