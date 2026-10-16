package com.ballastlane.pokedex.infrastructure.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo data settings ({@code app.seed.*}): whether to seed at startup (off unless switched on, the
 * demo passwords are public) and which file to seed from.
 */
@ConfigurationProperties("app.seed")
public record SeedProperties(Boolean enabled, String location) {

  public static final String DEFAULT_LOCATION = "classpath:seed/demo-data.json";

  public SeedProperties {
    enabled = enabled != null && enabled;
    location = location == null || location.isBlank() ? DEFAULT_LOCATION : location;
  }
}
