package com.ballastlane.pokedex.support;

import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.Username;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Real access tokens for tests, signed with the test profile's secret through the production {@link
 * TokenPort}, plus deliberately broken ones (expired, tampered, foreign issuer, foreign key) so
 * security tests exercise the real decoder instead of mocked principals.
 */
public final class TestTokens {

  private final TokenPort tokens;
  private final JwtEncoder encoder;

  public TestTokens(TokenPort tokens, JwtEncoder encoder) {
    this.tokens = tokens;
    this.encoder = encoder;
  }

  public static User user(String username, Role role) {
    return User.restore(
        UserId.random(),
        new Username(username),
        new EmailAddress(username + "@pallet.town"),
        new PasswordHash("$2a$04$not-a-real-hash"),
        role,
        Instant.parse("2026-10-14T12:00:00Z"),
        Instant.parse("2026-10-14T12:00:00Z"));
  }

  public String bearer(User user) {
    return "Bearer " + tokens.issue(user).value();
  }

  public String bearerFor(Role role) {
    return bearer(user(role.name().toLowerCase(Locale.ROOT) + "-user", role));
  }

  public String expired(Role role) {
    Instant past = Instant.now().minusSeconds(7200);
    return "Bearer " + sign(encoder, claims(role, "pokedex-api", past, past.plusSeconds(3600)));
  }

  public String fromForeignIssuer(Role role) {
    Instant now = Instant.now();
    return "Bearer " + sign(encoder, claims(role, "someone-else", now, now.plusSeconds(3600)));
  }

  public String signedWithForeignKey(Role role) {
    JwtEncoder foreign =
        NimbusJwtEncoder.withSecretKey(
                new SecretKeySpec(
                    "a-completely-different-secret-of-32-bytes!".getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"))
            .build();
    Instant now = Instant.now();
    return "Bearer " + sign(foreign, claims(role, "pokedex-api", now, now.plusSeconds(3600)));
  }

  /** A valid USER token whose payload was edited to claim ADMIN, keeping the old signature. */
  public String tamperedToAdmin() {
    String[] parts = bearerFor(Role.USER).substring("Bearer ".length()).split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();
    Base64.Encoder encoder64 = Base64.getUrlEncoder().withoutPadding();
    String payload =
        new String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
            .replace("\"role\":\"USER\"", "\"role\":\"ADMIN\"");
    return "Bearer "
        + parts[0]
        + "."
        + encoder64.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
        + "."
        + parts[2];
  }

  private static JwtClaimsSet claims(
      Role role, String issuer, Instant issuedAt, Instant expiresAt) {
    return JwtClaimsSet.builder()
        .issuer(issuer)
        .subject(UserId.random().value().toString())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .claim("username", "someone")
        .claim("role", role.name())
        .build();
  }

  private static String sign(JwtEncoder encoder, JwtClaimsSet claims) {
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
