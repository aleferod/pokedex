package com.ballastlane.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;

class PokeApiPropertiesTest {

  @Test
  void should_applySafeDefaults_when_onlyTheBaseUrlIsSet() {
    PokeApiProperties properties = bind(Map.of("base-url", "https://pokeapi.co/api/v2"));

    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(properties.maxConcurrency()).isEqualTo(8);
    assertThat(properties.userAgent())
        .isEqualTo("pokedex-api (+https://github.com/aleferod/pokedex)");
  }

  @Test
  void should_nameTheRepositoryInTheUserAgent_when_defaultConfigurationIsUsed() {
    // Given
    YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
    yaml.setResources(new ClassPathResource("application.yml"));

    // When
    Properties properties = yaml.getObject();

    // Then
    assertThat(properties).isNotNull();
    assertThat(properties.getProperty("app.pokeapi.user-agent"))
        .startsWith("pokedex-api/")
        .endsWith(" (+https://github.com/aleferod/pokedex)");
  }

  @Test
  void should_applyTheResilienceDefaults_when_nothingIsSet() {
    PokeApiProperties properties = bind(Map.of("base-url", "https://pokeapi.co/api/v2"));

    assertThat(properties.retry().maxAttempts()).isEqualTo(3);
    assertThat(properties.retry().initialBackoff()).isEqualTo(Duration.ofMillis(200));
    assertThat(properties.circuitBreaker().failureRateThreshold()).isEqualTo(50f);
    assertThat(properties.circuitBreaker().minimumCalls()).isEqualTo(10);
    assertThat(properties.circuitBreaker().slidingWindow()).isEqualTo(20);
    assertThat(properties.circuitBreaker().openDuration()).isEqualTo(Duration.ofSeconds(30));
  }

  @ParameterizedTest
  @CsvSource({
    "retry.max-attempts, 0",
    "retry.max-attempts, 11",
    "retry.initial-backoff, PT0S",
    "circuit-breaker.failure-rate-threshold, 0",
    "circuit-breaker.failure-rate-threshold, 101",
    "circuit-breaker.minimum-calls, 0",
    "circuit-breaker.sliding-window, 0",
    "circuit-breaker.minimum-calls, 21",
    "circuit-breaker.open-duration, PT0S"
  })
  void should_reject_when_aResilienceSettingIsNonsensical(String property, String value) {
    assertThatThrownBy(() -> bind(Map.of("base-url", "https://pokeapi.co/api/v2", property, value)))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining(property);
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
