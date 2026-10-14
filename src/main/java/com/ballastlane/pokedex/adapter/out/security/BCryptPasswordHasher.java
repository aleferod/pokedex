package com.ballastlane.pokedex.adapter.out.security;

import com.ballastlane.pokedex.application.port.out.user.PasswordHasherPort;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Adapter: {@link PasswordHasherPort} on BCrypt (salted, adaptive). The cost factor comes from
 * configuration: 10+ in real environments, 4 in tests to keep the suite fast.
 */
public class BCryptPasswordHasher implements PasswordHasherPort {

  private final BCryptPasswordEncoder encoder;

  public BCryptPasswordHasher(int strength) {
    this.encoder = new BCryptPasswordEncoder(strength);
  }

  @Override
  public PasswordHash hash(String rawPassword) {
    return new PasswordHash(encoder.encode(rawPassword));
  }

  @Override
  public boolean matches(String rawPassword, PasswordHash hash) {
    return encoder.matches(rawPassword, hash.value());
  }
}
