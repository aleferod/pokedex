package com.ballastlane.pokedex.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class CatalogCachePropertiesTest {

  @Test
  void should_keepEntriesForAnHourUpToTwoThousand_when_nothingIsSet() {
    CatalogCacheProperties properties = bind(Map.of());

    assertThat(properties.ttl()).isEqualTo(Duration.ofHours(1));
    assertThat(properties.maxEntries()).isEqualTo(2000L);
  }

  @Test
  void should_useTheConfiguredValues_when_set() {
    CatalogCacheProperties properties = bind(Map.of("ttl", "PT10M", "max-entries", "50"));

    assertThat(properties.ttl()).isEqualTo(Duration.ofMinutes(10));
    assertThat(properties.maxEntries()).isEqualTo(50L);
  }

  @ParameterizedTest
  @CsvSource({"ttl, PT0S", "ttl, -PT1M", "max-entries, 0", "max-entries, -5"})
  void should_reject_when_aSettingIsNotPositive(String property, String value) {
    assertThatThrownBy(() -> bind(Map.of(property, value)))
        .isInstanceOf(BindException.class)
        .rootCause()
        .hasMessageContaining("app.catalog-cache." + property);
  }

  private static CatalogCacheProperties bind(Map<String, String> values) {
    Map<String, String> prefixed = new HashMap<>();
    values.forEach((key, value) -> prefixed.put("app.catalog-cache." + key, value));
    return new Binder(new MapConfigurationPropertySource(prefixed))
        .bindOrCreate("app.catalog-cache", Bindable.of(CatalogCacheProperties.class));
  }
}
