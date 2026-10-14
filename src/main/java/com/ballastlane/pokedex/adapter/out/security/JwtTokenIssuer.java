package com.ballastlane.pokedex.adapter.out.security;

import com.ballastlane.pokedex.application.port.out.user.IssuedToken;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.domain.user.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Adapter: {@link TokenPort} issuing HS256-signed JWTs. Claims: {@code sub} (account id), {@code
 * username}, {@code role}, {@code iss}, {@code iat}, {@code exp}. Lifetime and issuer are passed in
 * as plain values so this adapter does not depend on the configuration layer.
 */
public class JwtTokenIssuer implements TokenPort {

  private final JwtEncoder encoder;
  private final Clock clock;
  private final Duration ttl;
  private final String issuer;

  public JwtTokenIssuer(JwtEncoder encoder, Clock clock, Duration ttl, String issuer) {
    this.encoder = encoder;
    this.clock = clock;
    this.ttl = ttl;
    this.issuer = issuer;
  }

  @Override
  public IssuedToken issue(User user) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(ttl);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(user.id().value().toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("username", user.username().value())
            .claim("role", user.role().name())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedToken(token, expiresAt);
  }
}
