package com.ballastlane.pokedex.infrastructure.web;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call the API cross-origin ({@code app.cors.allowed-origins}, from
 * {@code APP_CORS_ALLOWED_ORIGINS}). Value object: the compact constructor rejects wildcards and
 * anything that is not a plain {@code scheme://host[:port]} origin, so a misconfiguration stops the
 * application at startup instead of silently opening the API to every site.
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

  private static final Pattern ORIGIN = Pattern.compile("https?://[^/\\s]+");

  public CorsProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    for (String origin : allowedOrigins) {
      if (origin.contains("*")) {
        throw new IllegalArgumentException(
            "app.cors.allowed-origins: explicit origins are required, wildcards are not allowed"
                + " (got '"
                + origin
                + "')");
      }
      if (!ORIGIN.matcher(origin).matches()) {
        throw new IllegalArgumentException(
            "app.cors.allowed-origins: '"
                + origin
                + "' is not an origin; expected scheme://host[:port] with scheme http or https");
      }
    }
  }
}
