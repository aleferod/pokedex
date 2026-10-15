package com.ballastlane.pokedex.application.port.in.user;

import com.ballastlane.pokedex.domain.user.Role;

/** Input of {@link ProvisionUser}; raw values, validated by the domain. Password never printed. */
public record ProvisionUserCommand(String username, String email, String password, Role role) {

  @Override
  public String toString() {
    return "ProvisionUserCommand[username="
        + username
        + ", email="
        + email
        + ", password=***, role="
        + role
        + "]";
  }
}
