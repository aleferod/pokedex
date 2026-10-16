package com.ballastlane.pokedex.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.application.port.out.user.IssuedToken;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.Username;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenIssuerTest {

  private static final SecretKey KEY =
      new SecretKeySpec(
          "unit-test-signing-secret-of-at-least-32-bytes".getBytes(StandardCharsets.UTF_8),
          "HmacSHA256");
  private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

  private final JwtTokenIssuer issuer =
      new JwtTokenIssuer(
          NimbusJwtEncoder.withSecretKey(KEY).build(),
          Clock.fixed(NOW, ZoneOffset.UTC),
          Duration.ofMinutes(15),
          "pokedex-api");

  private final User ash =
      User.register(
          UserId.random(),
          new Username("ash"),
          new EmailAddress("ash@pallet.town"),
          new PasswordHash("$2a$04$hash"),
          NOW);

  @Test
  void should_issueSignedTokenIdentifyingTheAccount_when_userLogsIn() {
    // When
    IssuedToken token = issuer.issue(ash);
    Jwt jwt =
        NimbusJwtDecoder.withSecretKey(KEY)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .decode(token.value());

    // Then
    assertThat(jwt.getSubject()).isEqualTo(ash.id().value().toString());
    assertThat(jwt.getClaimAsString("username")).isEqualTo("ash");
    assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
    assertThat(jwt.getClaimAsString("iss")).isEqualTo("pokedex-api");
    assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
  }

  @Test
  void should_expireAfterConfiguredLifetime_when_issued() {
    // When
    IssuedToken token = issuer.issue(ash);
    Jwt jwt = NimbusJwtDecoder.withSecretKey(KEY).build().decode(token.value());

    // Then
    assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
    assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
  }
}
