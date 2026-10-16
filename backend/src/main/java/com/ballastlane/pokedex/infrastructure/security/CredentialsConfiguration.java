package com.ballastlane.pokedex.infrastructure.security;

import com.ballastlane.pokedex.adapter.out.security.BCryptPasswordHasher;
import com.ballastlane.pokedex.adapter.out.security.JwtTokenIssuer;
import com.ballastlane.pokedex.application.port.out.user.PasswordHasherPort;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Wires the credential adapters: one HMAC key shared by the token encoder (login) and the decoder
 * (every protected request), the role-to-authority mapping, and BCrypt hashing.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class CredentialsConfiguration {

  static final String ROLE_CLAIM = "role";
  static final String USERNAME_CLAIM = "username";

  @Bean
  SecretKey jwtSigningKey(JwtProperties properties) {
    return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
    return NimbusJwtEncoder.withSecretKey(jwtSigningKey).build();
  }

  /** Verifies signature (HS256 only), expiry/not-before (with the configured skew) and issuer. */
  @Bean
  JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<Jwt>(
            new JwtTimestampValidator(properties.clockSkew()),
            new JwtIssuerValidator(properties.issuer())));
    return decoder;
  }

  /** {@code role} claim -> {@code ROLE_<role>} authority; the username names the principal. */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName(USERNAME_CLAIM);
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          String role = jwt.getClaimAsString(ROLE_CLAIM);
          return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
    return converter;
  }

  @Bean
  TokenPort tokenIssuer(JwtEncoder jwtEncoder, Clock clock, JwtProperties properties) {
    return new JwtTokenIssuer(jwtEncoder, clock, properties.ttl(), properties.issuer());
  }

  @Bean
  PasswordHasherPort passwordHasher(
      @Value("${app.security.password.bcrypt-strength}") int bcryptStrength) {
    return new BCryptPasswordHasher(bcryptStrength);
  }
}
