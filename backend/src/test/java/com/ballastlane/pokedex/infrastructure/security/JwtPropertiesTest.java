package com.ballastlane.pokedex.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class JwtPropertiesTest {

  private static final String STRONG = "a-signing-secret-that-is-at-least-32-bytes";

  @Test
  void should_bindSecretLifetimeAndIssuer_when_valid() {
    JwtProperties properties = bind(STRONG, "PT15M");

    assertThat(properties.secret()).isEqualTo(STRONG);
    assertThat(properties.ttl()).isEqualTo(Duration.ofMinutes(15));
    assertThat(properties.issuer()).isEqualTo("pokedex-api");
  }

  @Test
  void should_defaultLifetimeToOneHourAndSkewTo30Seconds_when_notConfigured() {
    JwtProperties properties = bind(STRONG, null);

    assertThat(properties.ttl()).isEqualTo(Duration.ofHours(1));
    assertThat(properties.clockSkew()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void should_rejectStatingTheMinimum_when_secretIsShorterThan32Bytes() {
    assertThatThrownBy(() -> bind("only-16-chars-xx", null))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining("at least 32 bytes")
        .hasMessageContaining("JWT_SECRET");
  }

  @Test
  void should_reject_when_lifetimeIsNotPositive() {
    assertThatThrownBy(() -> bind(STRONG, "PT0S"))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining("positive");
  }

  private static JwtProperties bind(String secret, String ttl) {
    Map<String, String> values = new HashMap<>();
    values.put("app.security.jwt.secret", secret);
    values.put("app.security.jwt.issuer", "pokedex-api");
    if (ttl != null) {
      values.put("app.security.jwt.ttl", ttl);
    }
    return new Binder(new MapConfigurationPropertySource(values))
        .bind("app.security.jwt", JwtProperties.class)
        .get();
  }
}
