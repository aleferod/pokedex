package com.ballastlane.pokedex.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Access-token settings ({@code app.security.jwt.*}): signing secret, lifetime, issuer and the
 * clock skew tolerated when checking expiry (default 30 s). Value object: HMAC-SHA256 needs a key
 * of at least 256 bits, so a shorter secret stops the application at startup instead of producing
 * weakly signed tokens.
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer, Duration clockSkew) {

  public static final int MIN_SECRET_BYTES = 32;

  public JwtProperties {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("app.security.jwt.secret (JWT_SECRET) is required");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalArgumentException(
          "app.security.jwt.secret (JWT_SECRET) must be at least "
              + MIN_SECRET_BYTES
              + " bytes; generate one with: openssl rand -base64 48");
    }
    ttl = ttl == null ? Duration.ofHours(1) : ttl;
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("app.security.jwt.ttl (JWT_TTL) must be positive");
    }
    issuer = issuer == null || issuer.isBlank() ? "pokedex-api" : issuer;
    clockSkew = clockSkew == null ? Duration.ofSeconds(30) : clockSkew;
    if (clockSkew.isNegative()) {
      throw new IllegalArgumentException("app.security.jwt.clock-skew must not be negative");
    }
  }
}
