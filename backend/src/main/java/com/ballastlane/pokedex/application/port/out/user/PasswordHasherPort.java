package com.ballastlane.pokedex.application.port.out.user;

import com.ballastlane.pokedex.domain.user.PasswordHash;

/** One-way, salted password hashing. */
public interface PasswordHasherPort {

  PasswordHash hash(String rawPassword);

  boolean matches(String rawPassword, PasswordHash hash);
}
