package com.ballastlane.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class PokeApiPropertiesTest {

  @Test
  void should_applySafeDefaults_when_onlyTheBaseUrlIsSet() {
    PokeApiProperties properties = bind(Map.of("base-url", "https://pokeapi.co/api/v2"));

    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(properties.maxConcurrency()).isEqualTo(8);
    assertThat(properties.userAgent()).startsWith("pokedex-api");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "65", "-1"})
  void should_reject_when_maxConcurrencyIsOutsideOneToSixtyFour(String value) {
    assertThatThrownBy(
            () -> bind(Map.of("base-url", "https://pokeapi.co/api/v2", "max-concurrency", value)))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining("max-concurrency");
  }

  @ParameterizedTest
  @ValueSource(strings = {"read-timeout", "connect-timeout"})
  void should_reject_when_aTimeoutIsNotPositive(String timeout) {
    assertThatThrownBy(() -> bind(Map.of("base-url", "https://pokeapi.co/api/v2", timeout, "PT0S")))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining(timeout);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "ftp://pokeapi.co", "pokeapi.co/api/v2"})
  void should_reject_when_baseUrlIsNotHttp(String baseUrl) {
    assertThatThrownBy(() -> bind(Map.of("base-url", baseUrl)))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining("base-url");
  }

  private static PokeApiProperties bind(Map<String, String> values) {
    Map<String, String> prefixed = new HashMap<>();
    values.forEach((key, value) -> prefixed.put("app.pokeapi." + key, value));
    return new Binder(new MapConfigurationPropertySource(prefixed))
        .bind("app.pokeapi", PokeApiProperties.class)
        .get();
  }
}
