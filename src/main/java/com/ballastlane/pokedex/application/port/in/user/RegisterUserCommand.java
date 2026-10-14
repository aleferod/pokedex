package com.ballastlane.pokedex.application.port.in.user;

/** Input of {@link RegisterUser}; raw values, validated by the domain. Password never printed. */
public record RegisterUserCommand(String username, String email, String password) {

  @Override
  public String toString() {
    return "RegisterUserCommand[username=" + username + ", email=" + email + ", password=***]";
  }
}
