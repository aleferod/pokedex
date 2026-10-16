package com.ballastlane.pokedex.application.port.out.user;

import java.time.Instant;

/** A signed access token and when it stops being valid. The token itself is never printed. */
public record IssuedToken(String value, Instant expiresAt) {

  @Override
  public String toString() {
    return "IssuedToken[value=***, expiresAt=" + expiresAt + "]";
  }
}
