package com.ballastlane.pokedex.application.port.in.user;

/** Input of {@link AuthenticateUser}. Password never printed. */
public record Credentials(String username, String password) {

  @Override
  public String toString() {
    return "Credentials[username=" + username + ", password=***]";
  }
}
